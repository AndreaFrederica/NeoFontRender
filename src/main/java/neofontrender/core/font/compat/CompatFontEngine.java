package neofontrender.core.font.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.NeoFontRender;
import neofontrender.Tags;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.awt.GlyphInfo;
import neofontrender.core.font.awt.providers.AwtTtfGlyphProvider;
import neofontrender.core.font.cosmic.CosmicColorGlyphs;
import neofontrender.core.font.support.FontEngineScope;

import java.awt.font.LineMetrics;
import java.io.File;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Vanilla-compatibility glyph provider.
 *
 * <p>This engine does not lay out or draw strings. It only supplies what vanilla {@code
 * FontRenderer} lacks: per-code-point advance values and 16x16 glyph pages. Layout, formatting
 * codes, wrapping, bold, italic, shadow, underline/strikethrough and obfuscation all stay in
 * vanilla code; compat only replaces the two leaf draw calls and fills the width tables.</p>
 */
public final class CompatFontEngine implements AutoCloseable {

    private static final int PAGE_GLYPHS = 256;
    private static final int GRID = 16;
    private static final int INNER_PAD = 1;
    private static final int MAX_PAGE_SIDE = 8192;
    private final IResourceManager resourceManager;
    /**
     * Pages live in this engine's own registry, never in Minecraft's {@code TextureManager}.
     * See {@link CompatTextureManager} for why sharing that id space corrupts vanilla textures.
     */
    private final CompatTextureManager glyphTextures;
    private final List<AwtTtfGlyphProvider> providers = new ArrayList<>();
    private final float rasterScale;
    private final int gutter;
    private final boolean linearFilter;
    /** Page texture layout; core profiles cannot sample legacy {@code GL_ALPHA} pages. */
    private final CompatPageFormat pageFormat;
    private final float referenceBaseline;
    private final int pageLimit;

    private final int maxAscent;
    private final int maxDescent;
    private final int cell;
    private final int pitch;
    private final int pageSide;
    private final int ascentPixels;
    private final float ascentGui;
    private final float cellGui;

    private final short[] providerIndex = new short[65536];
    private final float[] advances = new float[65536];
    private final Map<FontRenderer, Snapshot> snapshots = new IdentityHashMap<>();
    private final LinkedHashMap<Integer, CompatPage> pages =
            new LinkedHashMap<Integer, CompatPage>(32, 0.75F, true);
    /** Cosmic page rasterization is kept for later work but must not be selectable yet. */
    private static final boolean COSMIC_PROVIDER_ENABLED = false;

    /** Color glyphs live in their own cache; AWT cannot rasterize color fonts at all. */
    private final CompatColorCache colorGlyphs;
    /** True while vanilla renders the drop-shadow pass; colored glyphs skip that pass. */
    private volatile boolean shadowPass;

    /** Provider-level split: AWT is monochrome-only, cosmic supplies mono and color glyphs. */
    private final boolean cosmicProvider;
    /** NaN = unknown, -1 = not covered, otherwise the advance in GUI pixels. */
    private final float[] cosmicAdvances = new float[65536];
    /** 0 = unknown, 1 = color glyph, 2 = monochrome: filled while rasterizing a page. */
    private final byte[] cosmicColor = new byte[65536];
    /** Rasters produced by the current page batch, kept for drawing and measuring. */
    private final java.util.LinkedHashMap<Integer, CosmicColorGlyphs.Glyph> cosmicGlyphCache =
            new java.util.LinkedHashMap<Integer, CosmicColorGlyphs.Glyph>(256, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(
                        java.util.Map.Entry<Integer, CosmicColorGlyphs.Glyph> eldest) {
                    return size() > 2048;
                }
            };

    private final String revisionTag;
    private final CompatDiskCache diskCache;
    private volatile boolean ready;
    private volatile boolean closed;
    private long lastScaleCheck;
    private float guiPixel = 0.0F;

    public CompatFontEngine(IResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        float requestedScale = NeofontrenderConfig.compatRasterScale();
        float oversample = NeofontrenderConfig.fontOversample();
        if (requestedScale <= 0.0F) {
            // Follow font.oversample, but cap the page budget: page memory grows with the square
            // of the raster scale, and compat pages cover whole Unicode blocks.
            requestedScale = Math.min(6.0F, oversample);
        }
        this.rasterScale = Math.max(1.0F, Math.min(8.0F,
                Math.min(requestedScale, oversample)));
        this.gutter = Math.max(0, Math.min(8, NeofontrenderConfig.compatGutter()));
        this.linearFilter = !"nearest".equalsIgnoreCase(NeofontrenderConfig.compatFilter());
        boolean opengl33 = CompatPageFormat.opengl33();
        String configuredFormat = NeofontrenderConfig.compatRasterFormat();
        this.pageFormat = CompatPageFormat.select(configuredFormat, opengl33);
        if ("swizzle".equalsIgnoreCase(configuredFormat) && !opengl33) {
            NeoFontRender.LOGGER.warn(
                    "Compat: compat.rasterFormat=swizzle needs OpenGL 3.3; using GL_ALPHA pages");
        }
        if ("alpha".equalsIgnoreCase(configuredFormat) && !opengl33) {
            NeoFontRender.LOGGER.info(
                    "Compat: using legacy GL_ALPHA pages (compat.rasterFormat=alpha)");
        }
        NeoFontRender.LOGGER.info("Compat: page format={} (OpenGL 3.3={}, configured={})",
                pageFormat, opengl33, configuredFormat);
        this.referenceBaseline = NeofontrenderConfig.fontReferenceBaseline();
        this.pageLimit = Math.max(1, Math.min(128, NeofontrenderConfig.compatPageCachePages()));
        this.revisionTag = Long.toHexString(System.nanoTime());
        this.glyphTextures = new CompatTextureManager("compat/" + this.revisionTag);
        this.colorGlyphs = new CompatColorCache(this::cosmicGlyph);
        this.cosmicProvider = COSMIC_PROVIDER_ENABLED
                && "cosmic".equalsIgnoreCase(NeofontrenderConfig.compatRasterBackend())
                && CosmicColorGlyphs.isAvailable(resourceManager);
        if (!cosmicProvider && !"awt".equalsIgnoreCase(NeofontrenderConfig.compatRasterBackend())) {
            NeoFontRender.LOGGER.warn(
                    "compat.rasterBackend='{}' is unavailable (Cosmic ABI {}); using AWT",
                    NeofontrenderConfig.compatRasterBackend(), CosmicColorGlyphs.abiVersion());
        }

        for (int index = 0; index < providerIndex.length; index++) {
            providerIndex[index] = -2;
            advances[index] = Float.NaN;
            cosmicAdvances[index] = Float.NaN;
        }

        boolean loaded = false;
        List<AwtTtfGlyphProvider> generic = new ArrayList<>();
        for (String family : NeofontrenderConfig.fontFamily()) {
            AwtTtfGlyphProvider provider = loadProvider(family, false);
            if (provider == null) {
                NeoFontRender.LOGGER.warn("Compat: skipped unavailable font '{}'", family);
                continue;
            }
            if (isColorEmoji(provider)) {
                provider.close();
                NeoFontRender.LOGGER.info("Compat: skipped color emoji font '{}' (monochrome mode)", family);
                continue;
            }
            if (isGenericFamily(provider)) {
                generic.add(provider);
            } else {
                providers.add(provider);
            }
            loaded = true;
            NeoFontRender.LOGGER.info("Compat: loaded AWT font '{}' (size={}, raster={})",
                    family, NeofontrenderConfig.fontSize(), rasterScale);
        }
        // Java logical families (Serif/SansSerif/Monospaced) resolve to whatever the JVM picks,
        // often a serif CJK face on Windows. Concrete bundled/system faces must win so compat
        // matches the modern engines' sans face.
        providers.addAll(generic);
        if (loaded) {
            AwtTtfGlyphProvider system = loadProvider(null, true);
            if (system != null) {
                providers.add(system);
                NeoFontRender.LOGGER.info("Compat: loaded Java SansSerif as the final fallback");
            }
        } else {
            AwtTtfGlyphProvider system = loadProvider(null, true);
            if (system != null) {
                providers.add(system);
                loaded = true;
                NeoFontRender.LOGGER.warn("Compat: no configured font loaded; using SansSerif");
            }
        }
        if (!loaded) {
            NeoFontRender.LOGGER.error("Compat: no usable font; keeping vanilla rendering");
            this.maxAscent = 0;
            this.maxDescent = 0;
            this.cell = 0;
            this.pitch = 0;
            this.pageSide = 0;
            this.ascentPixels = 0;
            this.ascentGui = 0.0F;
            this.cellGui = 0.0F;
            this.diskCache = null;
            this.ready = false;
            return;
        }

        int ascent = 0;
        int descent = 0;
        for (AwtTtfGlyphProvider provider : providers) {
            try {
                LineMetrics metrics = provider.awtFont()
                        .getLineMetrics("Ag", provider.fontRenderContext());
                ascent = Math.max(ascent, (int) Math.ceil(metrics.getAscent()));
                descent = Math.max(descent, (int) Math.ceil(metrics.getDescent()));
            } catch (Throwable error) {
                NeoFontRender.LOGGER.warn("Compat: failed to read line metrics", error);
            }
        }
        this.maxAscent = Math.max(1, Math.min(ascent,
                (int) Math.ceil(NeofontrenderConfig.fontSize() * rasterScale)));
        this.maxDescent = Math.max(1, Math.min(descent,
                Math.max(2, (int) Math.ceil(NeofontrenderConfig.fontSize() * rasterScale * 0.35F))));
        this.cell = maxAscent + maxDescent + INNER_PAD * 2;
        this.pitch = cell + gutter * 2;
        this.pageSide = GRID * pitch;
        this.ascentPixels = maxAscent + INNER_PAD;
        this.ascentGui = ascentPixels / rasterScale;
        this.cellGui = cell / rasterScale;

        if (pageSide > MAX_PAGE_SIDE) {
            NeoFontRender.LOGGER.error(
                    "Compat: page side {} exceeds the safe limit {}; lower compat.rasterScale",
                    pageSide, MAX_PAGE_SIDE);
            this.diskCache = null;
            this.ready = false;
            return;
        }
        NeoFontRender.LOGGER.info(
                "Compat geometry: cell={}px pitch={}px page={}px ascentGui={} cellGui={}",
                cell, pitch, pageSide, ascentGui, cellGui);

        CompatDiskCache cache = null;
        if (NeofontrenderConfig.compatDiskCache()) {
            try {
                File gameDir = Minecraft.getMinecraft().gameDir;
                if (gameDir != null) {
                    cache = new CompatDiskCache(gameDir, fingerprint(), pageSide,
                            NeofontrenderConfig.compatDiskMaxBytes());
                }
            } catch (Throwable error) {
                NeoFontRender.LOGGER.warn("Compat: disk cache disabled", error);
            }
        }
        this.diskCache = cache;
        this.ready = true;

        if (NeofontrenderConfig.compatPrefetchBasicLatin()) {
            try {
                pageFor('A');
            } catch (Throwable error) {
                NeoFontRender.LOGGER.warn("Compat: Basic Latin prefetch failed", error);
            }
        }
    }

    private static boolean isGenericFamily(AwtTtfGlyphProvider provider) {
        try {
            String family = provider.awtFont().getFamily(Locale.ROOT);
            if (family == null) {
                return false;
            }
            String lower = family.toLowerCase(Locale.ROOT);
            return "serif".equals(lower) || "sansserif".equals(lower)
                    || "monospaced".equals(lower) || lower.startsWith("dialog");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isColorEmoji(AwtTtfGlyphProvider provider) {
        try {
            String family = provider.awtFont().getFamily(Locale.ROOT);
            if (family == null) {
                return false;
            }
            String lower = family.toLowerCase(Locale.ROOT);
            return lower.contains("emoji");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private AwtTtfGlyphProvider loadProvider(String family, boolean allowDefault) {        try {
            return AwtTtfGlyphProvider.load(
                    resourceManager,
                    family,
                    NeofontrenderConfig.fontSize(),
                    rasterScale,
                    0.0F, 0.0F,
                    NeofontrenderConfig.fontBaselineShift(),
                    NeofontrenderConfig.fontAutoBaseline(),
                    NeofontrenderConfig.fontReferenceBaseline(),
                    NeofontrenderConfig.fontAntialias(),
                    NeofontrenderConfig.fontAntialiasMode(),
                    NeofontrenderConfig.fontFractionalMetrics(),
                    0,
                    NeofontrenderConfig.fontVariableWeight(),
                    allowDefault);
        } catch (Throwable error) {
            NeoFontRender.LOGGER.warn("Compat: failed to load font '{}'", family, error);
            return null;
        }
    }

    private String fingerprint() {
        StringBuilder material = new StringBuilder(256);
        material.append(Tags.VERSION).append('|')
                .append(3).append('|')
                .append(rasterScale).append('|')
                .append(NeofontrenderConfig.fontSize()).append('|')
                .append(NeofontrenderConfig.fontAntialiasMode()).append('|')
                .append(NeofontrenderConfig.fontFractionalMetrics()).append('|')
                .append(cell).append('|').append(pitch).append('|').append(pageSide).append('|')
                .append(gutter).append('|').append(linearFilter).append('|')
                .append(NeofontrenderConfig.compatRasterBackend());
        for (String family : NeofontrenderConfig.fontFamily()) {
            material.append('|').append(family);
            try {
                File file = NeofontrenderConfig.resolveFontFile(family);
                if (file.isFile()) {
                    material.append(':').append(file.length()).append(':')
                            .append(file.lastModified());
                }
            } catch (Throwable ignored) {
                // A system font has no file; its family name is enough for the fingerprint.
            }
        }
        return CompatDiskCache.fingerprint(material.toString());
    }

    public boolean isReady() {
        return ready && !closed;
    }

    public int pageCount() {
        synchronized (pages) {
            return pages.size();
        }
    }

    public int cellPixels() {
        return cell;
    }

    public int pitchPixels() {
        return pitch;
    }

    public int pageSidePixels() {
        return pageSide;
    }

    public int gutterPixels() {
        return gutter;
    }

    public int innerPadPixels() {
        return INNER_PAD;
    }

    public int ascentPixels() {
        return ascentPixels;
    }

    AwtTtfGlyphProvider providerAt(int codePoint) {
        int index = providerFor(codePoint);
        return index >= 0 && index < providers.size() ? providers.get(index) : null;
    }

    boolean isCovered(int codePoint) {
        return providerFor(codePoint) >= 0;
    }


    /** True when the engine rasterizes pages with cosmic instead of AWT. */
    public boolean cosmicProvider() {
        return cosmicProvider;
    }

    /** One native batch call for a whole page; results feed drawing and measuring. */
    java.util.Map<Integer, CosmicColorGlyphs.Glyph> cosmicPage(int pageIndex) {
        int[] points = new int[256];
        int base = pageIndex << 8;
        for (int index = 0; index < points.length; index++) {
            points[index] = base + index;
        }
        return cosmicPageFor(points);
    }

    /** Rasterizes one code point, served from the page batch cache when possible. */
    CosmicColorGlyphs.Glyph cosmicGlyph(int codePoint) {
        synchronized (cosmicGlyphCache) {
            CosmicColorGlyphs.Glyph cached = cosmicGlyphCache.get(codePoint);
            if (cached != null) return cached;
        }
        return cosmicPageFor(new int[]{codePoint}).get(codePoint);
    }

    private java.util.Map<Integer, CosmicColorGlyphs.Glyph> cosmicPageFor(int[] points) {
        float scale = Math.max(1.0F, Math.min(8.0F, rasterScale));
        java.util.Map<Integer, CosmicColorGlyphs.Glyph> glyphs =
                CosmicColorGlyphs.renderPage(points, NeofontrenderConfig.fontSize(), scale);
        synchronized (cosmicGlyphCache) {
            cosmicGlyphCache.putAll(glyphs);
        }
        return glyphs;
    }

    /** Page rasterization records the advance so measuring does not re-rasterize every glyph. */
    void recordCosmicAdvance(int codePoint, float advance) {
        if (codePoint < 0 || codePoint >= cosmicAdvances.length) return;
        cosmicAdvances[codePoint] = advance > 0.0F ? advance : -1.0F;
    }

    void recordCosmicColor(int codePoint, boolean color) {
        if (codePoint < 0 || codePoint >= cosmicColor.length) return;
        cosmicColor[codePoint] = (byte) (color ? 1 : 2);
    }

    private boolean isCosmicColor(int codePoint) {
        byte known = cosmicColor[codePoint];
        if (known != 0) return known == 1;
        boolean color = colorGlyphs.covers(codePoint);
        cosmicColor[codePoint] = (byte) (color ? 1 : 2);
        return color;
    }

    private boolean cosmicCovered(int codePoint) {
        if (codePoint < 0 || codePoint >= cosmicAdvances.length) return false;
        float cached = cosmicAdvances[codePoint];
        if (Float.isNaN(cached)) {
            CosmicColorGlyphs.Glyph glyph = cosmicGlyph(codePoint);
            cached = glyph == null ? -1.0F : glyph.advance();
            cosmicAdvances[codePoint] = cached > 0.0F ? cached : -1.0F;
        }
        return cached > 0.0F;
    }

    private float cosmicAdvance(int codePoint) {
        if (!cosmicCovered(codePoint)) return -1.0F;
        return cosmicAdvances[codePoint];
    }

    /** Coverage as the active provider sees it. */
    private boolean covered(int codePoint) {
        return cosmicProvider ? cosmicCovered(codePoint) : providerFor(codePoint) >= 0;
    }

    private int providerFor(int codePoint) {
        if (codePoint < 0 || codePoint > 0xFFFF) {
            return -1;
        }
        short cached = providerIndex[codePoint];
        if (cached != -2) {
            return cached;
        }
        int found = -1;
        if (isEligible(codePoint)) {
            for (int index = 0; index < providers.size(); index++) {
                AwtTtfGlyphProvider provider = providers.get(index);
                try {
                    if (provider.canDisplay(codePoint)) {
                        found = index;
                        break;
                    }
                } catch (Throwable ignored) {
                    // Try the next fallback.
                }
            }
        }
        providerIndex[codePoint] = (short) found;
        return found;
    }

    private static boolean isEligible(int codePoint) {
        if (codePoint < 0x20 || codePoint > 0xFFFF) {
            return false;
        }
        if (codePoint == ' ' || codePoint == 0xA0 || codePoint == 0xA7) {
            return false;
        }
        if (Character.isISOControl(codePoint) || Character.isSurrogate((char) codePoint)) {
            return false;
        }
        int type = Character.getType(codePoint);
        if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK
                || type == Character.COMBINING_SPACING_MARK) {
            return false;
        }
        return true;
    }

    private float advance(int codePoint) {
        if (cosmicProvider) {
            return cosmicAdvance(codePoint);
        }
        if (providerFor(codePoint) < 0) {
            return -1.0F;
        }
        float cached = advances[codePoint];
        if (!Float.isNaN(cached)) {
            return cached;
        }
        float value = -1.0F;
        try {
            GlyphInfo info = providerAt(codePoint).getGlyph(codePoint);
            if (info != null) {
                value = info.getAdvance();
            }
        } catch (Throwable ignored) {
            // Treat as uncovered.
        }
        advances[codePoint] = value;
        return value;
    }

    /**
     * Fills vanilla's width tables for a covered code point. Called from {@code getCharWidth}
     * before vanilla computes the width, and from the leaf draw hooks as a safety net.
     */
    public boolean ensureMetrics(FontRenderer font, int codePoint,
                                 int[] charWidth, byte[] glyphWidth) {
        if (!isReady() || font == null || charWidth == null || glyphWidth == null
                || codePoint < 0 || codePoint > 0xFFFF) {
            return false;
        }
        if (!covered(codePoint)) {
            return false;
        }
        float value = advance(codePoint);
        if (!(value > 0.01F)) {
            return false;
        }
        int ascii = VanillaAsciiSheet.indexOf((char) codePoint);
        if (!FontEngineScope.preserveMetrics(charWidth, ascii, glyphWidth, codePoint)) {
            synchronized (this) {
                snapshot(font, charWidth, glyphWidth);
            }
        }
        int right = encodeGlyphWidth(value);
        glyphWidth[codePoint] = (byte) right;
        if (ascii >= 0 && ascii < charWidth.length) {
            int width = Math.round(value);
            charWidth[ascii] = Math.max(1, Math.min(32, width));
        }
        return true;
    }

    /** Temporarily supplies original bitmap widths when F3 chooses vanilla over global compat. */
    public synchronized void prepareVanillaMetrics(FontRenderer font, String text) {
        if (FontEngineScope.current() != FontEngineScope.Mode.VANILLA || text == null) return;
        Snapshot original = snapshots.get(font);
        if (original == null) return;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            int ascii = VanillaAsciiSheet.indexOf(character);
            if (!FontEngineScope.preserveMetrics(original.charWidth, ascii,
                    original.glyphWidth, character)) continue;
            original.glyphWidth[character] = original.glyphWidthCopy[character];
            if (ascii >= 0 && ascii < original.charWidth.length) {
                original.charWidth[ascii] = original.charWidthCopy[ascii];
            }
        }
    }

    /** Vanilla's unicode advance formula from the encoded byte (left nibble is always 0 here). */
    public static float unicodeAdvance(byte encoded) {
        int value = encoded & 0xFF;
        int left = value >>> 4;
        int right = value & 15;
        return (right + 1 - left) / 2.0F + 1.0F;
    }

    private static int encodeGlyphWidth(float advanceGui) {
        int right = (int) Math.round(2.0 * (advanceGui - 1.0)) - 1;
        // Byte 0 means "no glyph" in vanilla, so covered glyphs must never encode to 0.
        return Math.max(1, Math.min(15, right));
    }

    private void snapshot(FontRenderer font, int[] charWidth, byte[] glyphWidth) {
        if (snapshots.containsKey(font)) {
            return;
        }
        snapshots.put(font, new Snapshot(charWidth, charWidth.clone(),
                glyphWidth, glyphWidth.clone()));
    }

    /**
     * Marks the pass vanilla is about to render. {@code shadow} is true for the offset dark pass;
     * color glyphs are drawn once, in the main pass.
     */
    public void beginTextPass(boolean shadow) {
        this.shadowPass = shadow;
    }

    /** Draws one glyph at the current vanilla pen position. Returns false when not covered. */
    public boolean draw(FontRenderer font, char character, boolean italic) {
        if (!isReady() || font == null) {
            return false;
        }
        int codePoint = character;
        if (cosmicProvider && isCosmicColor(codePoint)) {
            if (shadowPass) {
                return true;
            }
            return colorGlyphs.draw(codePoint, font.posX,
                    font.posY + referenceBaseline, 0xFFFFFFFF);
        }
        if (!cosmicProvider && providerFor(codePoint) < 0) {
            return false;
        }
        CompatPage page = pageFor(codePoint);
        if (page == null) {
            return false;
        }
        if (!glyphTextures.bind(page.location)) {
            return false;
        }

        int offset = (codePoint & 0xFF) * 4;
        int minX = page.inkBounds[offset];
        int minY = page.inkBounds[offset + 1];
        int maxX = page.inkBounds[offset + 2];
        int maxY = page.inkBounds[offset + 3];
        if (maxX <= minX || maxY <= minY) {
            return true;
        }

        float u0 = (float) (page.cellX(codePoint) + minX) / page.side;
        float v0 = (float) (page.cellY(codePoint) + minY) / page.side;
        float u1 = (float) (page.cellX(codePoint) + maxX) / page.side;
        float v1 = (float) (page.cellY(codePoint) + maxY) / page.side;

        float x0 = font.posX + minX / rasterScale;
        float y0 = font.posY + referenceBaseline - (page.ascentPixels - minY) / rasterScale;
        float x1 = font.posX + maxX / rasterScale;
        float y1 = font.posY + referenceBaseline + (maxY - page.ascentPixels) / rasterScale;
        float pixel = guiPixel();
        if (pixel > 0.0F) {
            float width = x1 - x0;
            float height = y1 - y0;
            x0 = Math.round(x0 / pixel) * pixel;
            y0 = Math.round(y0 / pixel) * pixel;
            x1 = x0 + width;
            y1 = y0 + height;
        }
        float slant = italic ? 1.0F : 0.0F;

        GlStateManager.glBegin(5);
        GlStateManager.glTexCoord2f(u0, v0);
        GlStateManager.glVertex3f(x0 + slant, y0, 0.0F);
        GlStateManager.glTexCoord2f(u0, v1);
        GlStateManager.glVertex3f(x0 - slant, y1, 0.0F);
        GlStateManager.glTexCoord2f(u1, v0);
        GlStateManager.glVertex3f(x1 + slant, y0, 0.0F);
        GlStateManager.glTexCoord2f(u1, v1);
        GlStateManager.glVertex3f(x1 - slant, y1, 0.0F);
        GlStateManager.glEnd();
        return true;
    }

    private float guiPixel() {
        long now = System.currentTimeMillis();
        if (now - lastScaleCheck > 1000L) {
            lastScaleCheck = now;
            try {
                net.minecraft.client.gui.ScaledResolution resolution =
                        new net.minecraft.client.gui.ScaledResolution(Minecraft.getMinecraft());
                guiPixel = 1.0F / Math.max(1, resolution.getScaleFactor());
            } catch (Throwable ignored) {
                guiPixel = 0.0F;
            }
        }
        return guiPixel;
    }

    private CompatPage pageFor(int codePoint) {
        if (closed || !ready) {
            return null;
        }
        int pageIndex = codePoint >>> 8;
        synchronized (pages) {
            CompatPage cached = pages.get(pageIndex);
            if (cached != null) {
                cached.lastUsed = System.nanoTime();
                return cached;
            }
        }
        byte[] mask = diskCache != null ? diskCache.read(pageIndex) : null;
        if (mask == null) {
            mask = CompatPageRasterizer.raster(this, pageIndex);
            if (mask == null) {
                return null;
            }
            if (diskCache != null) {
                diskCache.write(pageIndex, mask);
            }
        }
        CompatPage created;
        try {
            created = createPage(pageIndex, mask);
        } catch (Throwable error) {
            NeoFontRender.LOGGER.warn("Compat: could not upload page {}", pageIndex, error);
            return null;
        }
        synchronized (pages) {
            CompatPage existing = pages.get(pageIndex);
            if (existing != null) {
                // Another caller won the race after this page was uploaded. Drop the page we
                // created by identity; deleting "whatever is at this location" could free the
                // winner's name instead.
                glyphTextures.release(created.texture);
                return existing;
            }
            pages.put(pageIndex, created);
            glyphTextures.put(created.location, created.texture);
            trimPages();
        }
        return created;
    }

    private CompatPage createPage(int pageIndex, byte[] mask) {
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        CompatPageTexture texture;
        try {
            texture = new CompatPageTexture(mask, pageSide, linearFilter, pageFormat);
        } finally {
            // Uploading a page must not leave it bound: the main menu / panorama may draw a
            // background immediately after font setup and must not sample a glyph page.
            GlStateManager.bindTexture(previousTexture);
        }
        ResourceLocation location = new ResourceLocation("neofontrender",
                "compat/" + revisionTag + "/page_" + pageIndex);
        return new CompatPage(pageIndex, pageSide, cell, pitch, gutter, ascentPixels,
                rasterScale, mask, location, texture);
    }

    private void trimPages() {
        Iterator<Map.Entry<Integer, CompatPage>> iterator = pages.entrySet().iterator();
        while (pages.size() > pageLimit && iterator.hasNext()) {
            CompatPage page = iterator.next().getValue();
            iterator.remove();
            glyphTextures.release(page.location, page.texture);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        ready = false;
        synchronized (this) {
            for (Snapshot snapshot : snapshots.values()) {
                snapshot.restore();
            }
            snapshots.clear();
        }
        synchronized (pages) {
            glyphTextures.clear();
            pages.clear();
        }
        colorGlyphs.close();
        if (diskCache != null) {
            diskCache.close();
        }
    }

    private static final class Snapshot {
        final int[] charWidth;
        final int[] charWidthCopy;
        final byte[] glyphWidth;
        final byte[] glyphWidthCopy;

        Snapshot(int[] charWidth, int[] charWidthCopy, byte[] glyphWidth, byte[] glyphWidthCopy) {
            this.charWidth = charWidth;
            this.charWidthCopy = charWidthCopy;
            this.glyphWidth = glyphWidth;
            this.glyphWidthCopy = glyphWidthCopy;
        }

        void restore() {
            System.arraycopy(charWidthCopy, 0, charWidth, 0,
                    Math.min(charWidth.length, charWidthCopy.length));
            System.arraycopy(glyphWidthCopy, 0, glyphWidth, 0,
                    Math.min(glyphWidth.length, glyphWidthCopy.length));
        }
    }
}
