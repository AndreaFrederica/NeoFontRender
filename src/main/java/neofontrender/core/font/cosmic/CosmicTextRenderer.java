package neofontrender.core.font.cosmic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.NeoFontRender;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.support.FontRenderTuning;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** cosmic-text shaping/Swash rasterization with Minecraft's LWJGL2 texture submission. */
public final class CosmicTextRenderer implements TextRenderBackend {
    private static final int RASTER_MAGIC = 0x434F534D;
    private static final int[] COLOR_CODES = createColorCodes();

    private final TextureManager textureManager;
    private final Map<RenderKey, CosmicRenderedText> renderCache = new LinkedHashMap<>(128, 0.75F, true);
    private final Map<MeasureKey, Float> measureCache = new LinkedHashMap<>(256, 0.75F, true);
    private long engine;
    private int nextTextureId;
    private final String primaryFamily;
    private long renderCacheHits;
    private long renderCacheMisses;
    private long renderCacheEvictions;
    private long measureCacheHits;
    private long measureCacheMisses;
    private long measureCacheEvictions;
    private long nativeRasterCount;
    private long cacheOperations;

    public CosmicTextRenderer(TextureManager textureManager, IResourceManager resourceManager) throws IOException {
        this.textureManager = textureManager;
        List<LoadedFont> loadedFonts = loadConfiguredFonts(resourceManager);
        byte[][] fonts = new byte[loadedFonts.size()][];
        String[] aliases = new String[loadedFonts.size()];
        for (int i = 0; i < loadedFonts.size(); i++) {
            aliases[i] = loadedFonts.get(i).alias;
            fonts[i] = loadedFonts.get(i).data;
        }
        // The core package intentionally contains no bundled TTF files. cosmic-text/fontdb still
        // loads the operating system font database, so an empty byte-font list is a supported mode
        // and is also what lets the OS-provided color emoji font participate in fallback.
        // Keep the configured family name separate from byte-backed fallback fonts. Skia can
        // resolve a system family such as "JetBrains Mono" directly, while the old Cosmic bridge
        // silently skipped it and promoted the first bundled fallback (usually Sarasa) to primary.
        List<String> fallbackFamilies = NeofontrenderConfig.fontFamily();
        if (!fallbackFamilies.isEmpty() && fallbackFamilies.get(0).equals(NeofontrenderConfig.fontName())) {
            fallbackFamilies = fallbackFamilies.subList(1, fallbackFamilies.size());
        }
        engine = CosmicNative.createEngine(fonts, aliases, NeofontrenderConfig.fontName(),
                fallbackFamilies.toArray(new String[0]),
                NeofontrenderConfig.cosmicRegularFont(), NeofontrenderConfig.cosmicBoldFont(),
                NeofontrenderConfig.cosmicItalicFont(), NeofontrenderConfig.cosmicBoldItalicFont(),
                NeofontrenderConfig.cosmicVariantOverridesOnlySwitchFont(),
                NeofontrenderConfig.fontVariableWeight(),
                NeofontrenderConfig.fontSize(), Locale.getDefault().toLanguageTag());
        if (engine == 0L) {
            throw new IOException("cosmic-text returned a null engine");
        }
        primaryFamily = CosmicNative.primaryFamily(engine);
        NeoFontRender.LOGGER.info(
                "Cosmic renderer loaded {} font resources; primary family='{}'; faces=[regular={}, bold={}, italic={}, boldItalic={}]",
                loadedFonts.size(), primaryFamily,
                CosmicNative.resolvedFace(engine, 0), CosmicNative.resolvedFace(engine, 1),
                CosmicNative.resolvedFace(engine, 2), CosmicNative.resolvedFace(engine, 3));
        String warnings = CosmicNative.resolutionWarnings(engine);
        if (warnings != null && !warnings.isEmpty()) {
            NeoFontRender.LOGGER.warn("Cosmic font resolution warnings:\n{}", warnings);
        }
    }

    @Override
    public boolean isReady() {
        return engine != 0L;
    }

    @Override
    public synchronized float measure(String text, boolean bold, boolean italic) {
        if (text == null || text.isEmpty() || engine == 0L) {
            return 0.0F;
        }
        MeasureKey key = new MeasureKey(text, effectiveFlags(bold, italic));
        Float cached = measureCache.get(key);
        if (cached != null) {
            measureCacheHits++;
            periodicCacheCleanup();
            return cached;
        }
        measureCacheMisses++;
        float width = CosmicNative.measure(engine, text, key.flags);
        measureCache.put(key, width);
        trimMeasureCache();
        periodicCacheCleanup();
        return width;
    }

    @Override
    public synchronized TextRenderResult render(String text, int argb, boolean bold, boolean italic) {
        if (text == null || text.isEmpty() || engine == 0L) {
            return TextRenderResult.EMPTY;
        }
        float scale = Math.max(1.0F, FontRenderTuning.rasterScale(NeofontrenderConfig.fontOversample()));
        // Minecraft applies the caller alpha through vertex color during draw. Keeping the cached
        // raster opaque avoids multiplying that alpha a second time and also lets alpha variants
        // share the same native raster/GL texture.
        int rasterArgb = argb | 0xFF000000;
        RenderKey key = new RenderKey(text, rasterArgb, effectiveFlags(bold, italic), Float.floatToIntBits(scale));
        CosmicRenderedText cached = renderCache.get(key);
        if (cached != null) {
            renderCacheHits++;
            cached.touch();
            periodicCacheCleanup();
            return cached;
        }
        renderCacheMisses++;
        byte[] encoded = CosmicNative.render(engine, text, rasterArgb, key.flags, scale);
        nativeRasterCount++;
        CosmicRenderedText rendered = decode(encoded);
        renderCache.put(key, rendered);
        trimRenderCache();
        periodicCacheCleanup();
        return rendered;
    }

    @Override
    public float measureFormatted(String text, int baseArgb, boolean shadow) {
        float width = 0.0F;
        for (FormattedRun run : parseFormatted(text, baseArgb, shadow)) {
            width += measure(run.text, run.bold, run.italic);
        }
        return width;
    }

    @Override
    public TextRenderResult renderFormatted(String text, int baseArgb, boolean shadow) {
        List<PositionedResult> results = new ArrayList<>();
        float x = 0.0F;
        for (FormattedRun run : parseFormatted(text, baseArgb, shadow)) {
            TextRenderResult rendered = render(run.text, run.argb, run.bold, run.italic);
            results.add(new PositionedResult(x, rendered));
            x += rendered.advance();
        }
        return results.isEmpty() ? TextRenderResult.EMPTY : new CompositeResult(results, x);
    }

    @Override
    public void prewarmBasicLatin() {
        measure("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789", false, false);
    }

    private CosmicRenderedText decode(byte[] encoded) {
        if (encoded == null || encoded.length < 32) {
            throw new IllegalStateException("cosmic-text returned a truncated raster");
        }
        ByteBuffer data = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        if (data.getInt() != RASTER_MAGIC) {
            throw new IllegalStateException("cosmic-text returned an invalid raster header");
        }
        int width = data.getInt();
        int height = data.getInt();
        int offsetX = data.getInt();
        int offsetY = data.getInt();
        float advance = data.getFloat();
        float baseline = data.getFloat();
        float scale = data.getFloat();
        long pixelCount = (long) width * height;
        if (width < 0 || height < 0 || pixelCount > Integer.MAX_VALUE || data.remaining() != pixelCount * 4L) {
            // Validate all native dimensions before allocation. A mismatched DLL should degrade to
            // a backend error instead of causing an uncontrolled Java heap allocation.
            throw new IllegalStateException("cosmic-text returned invalid dimensions " + width + "x" + height);
        }
        if (width == 0 || height == 0) {
            return new CosmicRenderedText(null, null, advance, 0.0F, 0.0F, 0.0F, 0.0F, scale);
        }
        DynamicTexture texture = new DynamicTexture(width, height);
        int[] pixels = texture.getTextureData();
        for (int index = 0; index < pixels.length; index++) {
            pixels[index] = data.getInt();
        }
        texture.updateDynamicTexture();
        FontRenderTuning.applyFontTextureFilter(texture, scale, false);
        ResourceLocation location = new ResourceLocation("neofontrender", "cosmic/" + nextTextureId++);
        textureManager.loadTexture(location, texture);
        FontRenderTuning.applyFontTextureFilter(texture, scale, false);
        return new CosmicRenderedText(location, texture, advance, width / scale, height / scale,
                offsetX / scale,
                offsetY / scale + NeofontrenderConfig.fontReferenceBaseline()
                        + NeofontrenderConfig.fontBaselineShift() - baseline,
                scale);
    }

    private List<LoadedFont> loadConfiguredFonts(IResourceManager resourceManager) throws IOException {
        LinkedHashMap<String, Boolean> selectors = new LinkedHashMap<>();
        selectors.put(NeofontrenderConfig.fontName(), Boolean.TRUE);
        for (String name : NeofontrenderConfig.cosmicFaceOverrides()) {
            if (name != null && !name.trim().isEmpty()) {
                selectors.put(name, Boolean.TRUE);
            }
        }
        for (String name : NeofontrenderConfig.fontFamily()) {
            selectors.put(name, Boolean.TRUE);
        }

        List<LoadedFont> fonts = new ArrayList<>();
        for (String name : selectors.keySet()) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            try {
                File file = new File(name);
                if (file.isFile()) {
                    try (InputStream input = new FileInputStream(file)) {
                        fonts.add(new LoadedFont(name, readAllBytes(input)));
                    }
                } else if (name.indexOf(':') >= 0) {
                    IResource resource = resourceManager.getResource(new ResourceLocation(name));
                    try (InputStream input = resource.getInputStream()) {
                        fonts.add(new LoadedFont(name, readAllBytes(input)));
                    }
                }
            } catch (IOException error) {
                // Core intentionally omits bundled TTF resources. A configured resource from
                // the full/resources package must not make Cosmic fail completely: the native
                // engine can resolve the configured family and emoji through the OS font DB.
                NeoFontRender.LOGGER.warn("Skipped unavailable Cosmic font resource '{}'", name);
            }
        }
        return fonts;
    }

    private static final class LoadedFont {
        private final String alias;
        private final byte[] data;

        private LoadedFont(String alias, byte[] data) {
            this.alias = alias;
            this.data = data;
        }
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] block = new byte[8192];
        int read;
        while ((read = input.read(block)) >= 0) {
            output.write(block, 0, read);
        }
        return output.toByteArray();
    }

    private static List<FormattedRun> parseFormatted(String text, int baseArgb, boolean shadow) {
        List<FormattedRun> runs = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return runs;
        }
        int color = normalizeAlpha(baseArgb);
        boolean bold = false;
        boolean italic = false;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '\u00a7' || i + 1 >= text.length()) {
                continue;
            }
            if (i > start) {
                runs.add(new FormattedRun(text.substring(start, i), shadowColor(color, shadow), bold, italic));
            }
            char code = Character.toLowerCase(text.charAt(++i));
            int colorIndex = "0123456789abcdef".indexOf(code);
            if (colorIndex >= 0) {
                color = (baseArgb & 0xFF000000) | COLOR_CODES[colorIndex];
                bold = false;
                italic = false;
            } else if (code == 'l') {
                bold = true;
            } else if (code == 'o') {
                italic = true;
            } else if (code == 'r') {
                color = normalizeAlpha(baseArgb);
                bold = false;
                italic = false;
            }
            start = i + 1;
        }
        if (start < text.length()) {
            runs.add(new FormattedRun(text.substring(start), shadowColor(color, shadow), bold, italic));
        }
        return runs;
    }

    private static int normalizeAlpha(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }

    private static int shadowColor(int color, boolean shadow) {
        return shadow ? (color & 0xFF000000) | ((color & 0xFCFCFC) >> 2) : color;
    }

    private static int[] createColorCodes() {
        int[] codes = new int[16];
        for (int index = 0; index < 16; index++) {
            int intensity = (index >> 3 & 1) * 85;
            int red = (index >> 2 & 1) * 170 + intensity;
            int green = (index >> 1 & 1) * 170 + intensity;
            int blue = (index & 1) * 170 + intensity;
            if (index == 6) {
                red += 85;
            }
            codes[index] = red << 16 | green << 8 | blue;
        }
        return codes;
    }

    private static int effectiveFlags(boolean bold, boolean italic) {
        int configuredStyle = NeofontrenderConfig.fontStyle();
        return (bold || (configuredStyle & 1) != 0 ? 1 : 0)
                | (italic || (configuredStyle & 2) != 0 ? 2 : 0);
    }

    private void trimRenderCache() {
        int max = Math.max(1, NeofontrenderConfig.textCacheMaxEntries());
        Iterator<Map.Entry<RenderKey, CosmicRenderedText>> iterator = renderCache.entrySet().iterator();
        while (renderCache.size() > max && iterator.hasNext()) {
            Map.Entry<RenderKey, CosmicRenderedText> eldest = iterator.next();
            eldest.getValue().close();
            iterator.remove();
            renderCacheEvictions++;
        }

        long ttlMillis = (long) (NeofontrenderConfig.textCacheTtlSeconds() * 1000.0F);
        if (ttlMillis <= 0L) {
            return;
        }
        int min = Math.max(0, Math.min(max, NeofontrenderConfig.textCacheMinEntries()));
        long now = System.currentTimeMillis();
        iterator = renderCache.entrySet().iterator();
        while (renderCache.size() > min && iterator.hasNext()) {
            Map.Entry<RenderKey, CosmicRenderedText> eldest = iterator.next();
            if (!eldest.getValue().isExpired(now, ttlMillis)) {
                break;
            }
            eldest.getValue().close();
            iterator.remove();
            renderCacheEvictions++;
        }
    }

    private void trimMeasureCache() {
        int max = Math.max(1, NeofontrenderConfig.measureCacheMaxEntries());
        Iterator<MeasureKey> iterator = measureCache.keySet().iterator();
        while (measureCache.size() > max && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
            measureCacheEvictions++;
        }
    }

    private void periodicCacheCleanup() {
        // TTL must also progress during a stable screen where every draw is a cache hit. Checking
        // once per 256 operations keeps that guarantee without walking the LRU on every frame.
        if ((++cacheOperations & 255L) == 0L) {
            trimRenderCache();
            trimMeasureCache();
        }
    }

    public synchronized DebugState debugState() {
        return new DebugState(primaryFamily, renderCache.size(), NeofontrenderConfig.textCacheMaxEntries(),
                measureCache.size(), NeofontrenderConfig.measureCacheMaxEntries(),
                renderCacheHits, renderCacheMisses, renderCacheEvictions,
                measureCacheHits, measureCacheMisses, measureCacheEvictions, nativeRasterCount);
    }

    @Override
    public synchronized void close() {
        for (CosmicRenderedText rendered : renderCache.values()) {
            rendered.close();
        }
        renderCache.clear();
        measureCache.clear();
        if (engine != 0L) {
            CosmicNative.destroyEngine(engine);
            engine = 0L;
        }
    }

    private static final class CosmicRenderedText implements TextRenderResult, AutoCloseable {
        private final ResourceLocation location;
        private final DynamicTexture texture;
        private final float advance;
        private final float width;
        private final float height;
        private final float offsetX;
        private final float offsetY;
        private final float scale;
        private volatile long lastAccessMillis;

        private CosmicRenderedText(ResourceLocation location, DynamicTexture texture, float advance,
                                   float width, float height, float offsetX, float offsetY, float scale) {
            this.location = location;
            this.texture = texture;
            this.advance = advance;
            this.width = width;
            this.height = height;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.scale = scale;
            this.lastAccessMillis = System.currentTimeMillis();
        }

        @Override
        public float advance() {
            return advance;
        }

        private void touch() {
            lastAccessMillis = System.currentTimeMillis();
        }

        private boolean isExpired(long now, long ttlMillis) {
            return now - lastAccessMillis >= ttlMillis;
        }

        @Override
        public void draw(float x, float y, float alpha) {
            if (location == null || texture == null || width <= 0.0F || height <= 0.0F) {
                return;
            }
            Minecraft.getMinecraft().getTextureManager().bindTexture(location);
            FontRenderTuning.applyBoundTextureFilter(scale, false);
            // DynamicTexture inherits GL_REPEAT. Cosmic rasters are line textures rather than an
            // atlas, so repeating the first row at v=1 creates bright specks below glyphs when
            // GL_LINEAR samples the edge. Reassert clamping because other renderers may mutate the
            // currently bound texture parameters between cached draws.
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
            float left = FontRenderTuning.alignToPixel(x + offsetX);
            float top = FontRenderTuning.alignToPixel(y + offsetY);
            // Cosmic textures are premultiplied in Rust before GL_LINEAR minification. Force the
            // matching blend function here because surrounding mods frequently leave Minecraft's
            // cached blend state configured for straight-alpha GUI textures.
            try (PremultipliedBlendState ignored = new PremultipliedBlendState()) {
                Tessellator tessellator = Tessellator.instance;
                tessellator.startDrawingQuads();
                tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, alpha);
                tessellator.addVertexWithUV(left, top, 0.0D, 0.0D, 0.0D);
                tessellator.addVertexWithUV(left, top + height, 0.0D, 0.0D, 1.0D);
                tessellator.addVertexWithUV(left + width, top + height, 0.0D, 1.0D, 1.0D);
                tessellator.addVertexWithUV(left + width, top, 0.0D, 1.0D, 0.0D);
                tessellator.draw();
            }
        }

        @Override
        public void close() {
            if (location != null) {
                Minecraft.getMinecraft().getTextureManager().deleteTexture(location);
            }
        }
    }

    public static final class DebugState {
        public final String primaryFamily;
        public final int renderCacheSize;
        public final int renderCacheMax;
        public final int measureCacheSize;
        public final int measureCacheMax;
        public final long renderHits;
        public final long renderMisses;
        public final long renderEvictions;
        public final long measureHits;
        public final long measureMisses;
        public final long measureEvictions;
        public final long nativeRasterCount;

        private DebugState(String primaryFamily, int renderCacheSize, int renderCacheMax,
                           int measureCacheSize, int measureCacheMax,
                           long renderHits, long renderMisses, long renderEvictions,
                           long measureHits, long measureMisses, long measureEvictions,
                           long nativeRasterCount) {
            this.primaryFamily = primaryFamily;
            this.renderCacheSize = renderCacheSize;
            this.renderCacheMax = renderCacheMax;
            this.measureCacheSize = measureCacheSize;
            this.measureCacheMax = measureCacheMax;
            this.renderHits = renderHits;
            this.renderMisses = renderMisses;
            this.renderEvictions = renderEvictions;
            this.measureHits = measureHits;
            this.measureMisses = measureMisses;
            this.measureEvictions = measureEvictions;
            this.nativeRasterCount = nativeRasterCount;
        }
    }

    private static final class PremultipliedBlendState implements AutoCloseable {
        private final boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);

        private PremultipliedBlendState() {
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ZERO);
        }

        @Override
        public void close() {
            GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (!blendEnabled) {
                GL11.glDisable(GL11.GL_BLEND);
            }
        }
    }

    private static final class CompositeResult implements TextRenderResult {
        private final List<PositionedResult> parts;
        private final float advance;

        private CompositeResult(List<PositionedResult> parts, float advance) {
            this.parts = parts;
            this.advance = advance;
        }

        @Override
        public float advance() {
            return advance;
        }

        @Override
        public void draw(float x, float y, float alpha) {
            for (PositionedResult part : parts) {
                part.result.draw(x + part.x, y, alpha);
            }
        }
    }

    private static final class PositionedResult {
        private final float x;
        private final TextRenderResult result;

        private PositionedResult(float x, TextRenderResult result) {
            this.x = x;
            this.result = result;
        }
    }

    private static final class FormattedRun {
        private final String text;
        private final int argb;
        private final boolean bold;
        private final boolean italic;

        private FormattedRun(String text, int argb, boolean bold, boolean italic) {
            this.text = text;
            this.argb = argb;
            this.bold = bold;
            this.italic = italic;
        }
    }

    private static final class RenderKey {
        private final String text;
        private final int argb;
        private final int flags;
        private final int scale;

        private RenderKey(String text, int argb, int flags, int scale) {
            this.text = text;
            this.argb = argb;
            this.flags = flags;
            this.scale = scale;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof RenderKey)) return false;
            RenderKey other = (RenderKey) object;
            return argb == other.argb && flags == other.flags && scale == other.scale && text.equals(other.text);
        }

        @Override
        public int hashCode() {
            int hash = text.hashCode();
            hash = 31 * hash + argb;
            hash = 31 * hash + flags;
            return 31 * hash + scale;
        }
    }

    private static final class MeasureKey {
        private final String text;
        private final int flags;

        private MeasureKey(String text, int flags) {
            this.text = text;
            this.flags = flags;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof MeasureKey && flags == ((MeasureKey) object).flags && text.equals(((MeasureKey) object).text);
        }

        @Override
        public int hashCode() {
            return 31 * text.hashCode() + flags;
        }
    }
}
