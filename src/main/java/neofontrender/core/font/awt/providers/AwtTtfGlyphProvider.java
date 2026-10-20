package neofontrender.core.font.awt.providers;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.build.BuildFeatures;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.awt.BakedGlyph;
import neofontrender.core.font.awt.FontTexture;
import neofontrender.core.font.awt.GlyphInfo;
import neofontrender.core.font.awt.GlyphProvider;
import neofontrender.core.font.support.FontPixelUtils;
import neofontrender.core.font.support.FontRenderTuning;

import javax.annotation.Nullable;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link GlyphProvider} that renders TrueType fonts using AWT.
 *
 * <p>This is the 1.12.2 equivalent of 1.20.1's {@code TrueTypeGlyphProvider},
 * which uses STBTruetype. AWT is used here because STB is not available
 * in the 1.12.2 Forge classpath.</p>
 */
public class AwtTtfGlyphProvider implements GlyphProvider {

    private static final int MAX_LAYOUT_CACHE_SIZE = 4096;

    private final Font font;
    private final float size;
    private final float oversample;
    private final float shiftX;
    private final float shiftY;
    private final float baselineShift;
    private final boolean autoBaseline;
    private final float referenceBaseline;
    private final boolean antialias;
    private final Object antialiasHint;
    private final boolean fractionalMetrics;
    private long layoutCacheHits;
    private long layoutCacheMisses;
    private long layoutCacheEvictions;
    /** WIP until the in-game benchmark demonstrates a benefit for the active font. */
    private volatile boolean fixedLayoutOptimization = false;
    private final Map<FixedLayoutKey, FixedLayoutProfile> fixedLayoutCache = Collections.synchronizedMap(
            new LinkedHashMap<FixedLayoutKey, FixedLayoutProfile>(128, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<FixedLayoutKey, FixedLayoutProfile> eldest) {
                    return size() > 512;
                }
            });
    private final Map<LayoutKey, float[]> layoutCache = Collections.synchronizedMap(
            new LinkedHashMap<LayoutKey, float[]>(MAX_LAYOUT_CACHE_SIZE, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<LayoutKey, float[]> eldest) {
                    if (size() > MAX_LAYOUT_CACHE_SIZE) {
                        if (BuildFeatures.RENDER_STATS) layoutCacheEvictions++;
                        return true;
                    }
                    return false;
                }
            });

    private AwtTtfGlyphProvider(Font font, float size, float oversample, float shiftX, float shiftY,
                                float baselineShift, boolean autoBaseline, float referenceBaseline,
                                boolean antialias, String antialiasMode, boolean fractionalMetrics) {
        this.font = font;
        this.size = size;
        this.oversample = oversample;
        this.shiftX = shiftX * oversample;
        this.shiftY = shiftY * oversample;
        this.baselineShift = baselineShift;
        this.autoBaseline = autoBaseline;
        this.referenceBaseline = referenceBaseline;
        this.antialiasHint = antialiasHint(antialias, antialiasMode);
        this.antialias = this.antialiasHint != RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
        this.fractionalMetrics = fractionalMetrics;
    }

    /** The AWT font derived at {@code size * oversample}; exposed for the compat page rasterizer. */
    public Font awtFont() {
        return font;
    }

    public float logicalSize() {
        return size;
    }

    public float oversample() {
        return oversample;
    }

    public boolean fractionalMetrics() {
        return fractionalMetrics;
    }

    public Object textAntialiasHint() {
        return antialiasHint;
    }

    /** Hints matching this provider's rasterization, used by the compat page rasterizer. */
    public FontRenderContext fontRenderContext() {
        return new FontRenderContext(null, antialias, fractionalMetrics);
    }

    @Nullable
    public static AwtTtfGlyphProvider load(IResourceManager manager,
                                           @Nullable String fontName,
                                           float size, float oversample,
                                           float shiftX, float shiftY,
                                           float baselineShift,
                                           boolean autoBaseline,
                                           float referenceBaseline,
                                           boolean antialias,
                                           boolean fractionalMetrics,
                                           int style) throws IOException {
        return load(manager, fontName, size, oversample, shiftX, shiftY, baselineShift, autoBaseline,
                referenceBaseline, antialias, antialias ? "on" : "off", fractionalMetrics, style, 0, true);
    }

    @Nullable
    public static AwtTtfGlyphProvider load(IResourceManager manager,
                                           @Nullable String fontName,
                                           float size, float oversample,
                                           float shiftX, float shiftY,
                                           float baselineShift,
                                           boolean autoBaseline,
                                           float referenceBaseline,
                                           boolean antialias,
                                           String antialiasMode,
                                           boolean fractionalMetrics,
                                           int style,
                                           int variableWeight,
                                           boolean allowDefaultFallback) throws IOException {
        Font baseFont;
        if (fontName != null && !fontName.isEmpty()) {
            File fontFile = NeofontrenderConfig.resolveFontFile(fontName);
            if (fontFile.isFile()) {
                try {
                    baseFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                    baseFont = baseFont.deriveFont(style, 1.0F);
                } catch (java.awt.FontFormatException e) {
                    throw new IOException("Failed to load local font file " + fontFile, e);
                }
            } else {
                baseFont = new Font(fontName, style, 1);
            }
            if (baseFont.getFamily().equals("Dialog") && !fontName.equalsIgnoreCase("Dialog") && !fontFile.isFile()) {
                if (manager != null) {
                    try {
                        ResourceLocation location = new ResourceLocation(fontName);
                        IResource resource = manager.getResource(location);
                        try (InputStream stream = resource.getInputStream()) {
                            baseFont = Font.createFont(Font.TRUETYPE_FONT, stream);
                        } catch (java.awt.FontFormatException e) {
                            throw new IOException("Failed to load TTF font from " + fontName, e);
                        }
                    } catch (Exception e) {
                        if (!allowDefaultFallback) {
                            return null;
                        }
                        baseFont = new Font(Font.SANS_SERIF, style, 1);
                    }
                } else {
                    if (!allowDefaultFallback) {
                        return null;
                    }
                    baseFont = new Font(Font.SANS_SERIF, style, 1);
                }
            }
        } else {
            if (!allowDefaultFallback) {
                return null;
            }
            baseFont = new Font(Font.SANS_SERIF, style, 1);
        }

        float derivedSize = size * oversample;
        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        if (variableWeight > 0) {
            attributes.put(TextAttribute.WEIGHT, Math.max(0.1F, Math.min(2.5F, variableWeight / 400.0F)));
        }
        Font derived = baseFont.deriveFont(derivedSize).deriveFont(attributes);
        return new AwtTtfGlyphProvider(derived, size, oversample, shiftX, shiftY, baselineShift,
                autoBaseline, referenceBaseline, antialias, antialiasMode, fractionalMetrics);
    }

    @Nullable
    public static AwtTtfGlyphProvider load(IResourceManager manager,
                                           @Nullable ResourceLocation location,
                                           float size, float oversample,
                                           float shiftX, float shiftY) throws IOException {
        String name = location != null ? location.toString() : null;
        return load(manager, name, size, oversample, shiftX, shiftY, 0.0F, true, 7.0F,
                true, true, Font.PLAIN);
    }

    @Override
    public GlyphInfo getGlyph(int codePoint) {
        if (!font.canDisplay(codePoint)) {
            return null;
        }
        return new AwtGlyphInfo(codePoint);
    }

    @Override
    public Collection<Integer> getSupportedGlyphs() {
        return Collections.emptyList();
    }

    public boolean canDisplayText(String text) {
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (!font.canDisplay(cp)) {
                return false;
            }
            i += Character.charCount(cp);
        }
        return true;
    }

    public boolean canDisplay(int codePoint) {
        return font.canDisplay(codePoint);
    }

    public float[] layoutPositions(String text, boolean bold) {
        LayoutKey key = new LayoutKey(text, bold);
        float[] cached = layoutCache.get(key);
        if (cached != null) {
            if (BuildFeatures.RENDER_STATS) layoutCacheHits++;
            return cached;
        }
        if (BuildFeatures.RENDER_STATS) layoutCacheMisses++;

        float[] fixed = fixedLayoutOptimization ? fixedCharacterPositions(text, bold) : null;
        if (fixed != null) {
            layoutCache.put(key, fixed);
            return fixed;
        }
        float[] positions = layoutPositionsExact(text, bold);
        layoutCache.put(key, positions);
        return positions;
    }

    private float[] layoutPositionsExact(String text, boolean bold) {
        int len = text.length();
        float[] positions = new float[len + 1];
        if (len == 0) {
            return positions;
        }

        FontRenderContext frc = new FontRenderContext(null, antialias, fractionalMetrics);
        char[] chars = text.toCharArray();
        GlyphVector gv = font.layoutGlyphVector(frc, chars, 0, chars.length, Font.LAYOUT_LEFT_TO_RIGHT);

        float current = 0.0F;
        for (int i = 0; i < len; ) {
            int cp = text.codePointAt(i);
            GlyphInfo info = getGlyph(cp);
            current += info == null ? 0.0F : info.getAdvance();
            int next = i + Character.charCount(cp);
            for (int pos = i + 1; pos <= next && pos < positions.length; pos++) {
                positions[pos] = current;
            }
            i = next;
        }

        boolean[] set = new boolean[len + 1];
        for (int glyph = 0; glyph < gv.getNumGlyphs(); glyph++) {
            int charIndex = gv.getGlyphCharIndex(glyph);
            if (charIndex >= 0 && charIndex < len) {
                positions[charIndex] = (float) gv.getGlyphPosition(glyph).getX() / oversample;
                set[charIndex] = true;
            }
        }

        positions[len] = (float) gv.getGlyphPosition(gv.getNumGlyphs()).getX() / oversample;
        set[len] = true;

        for (int i = 0; i < len; ) {
            int cp = text.codePointAt(i);
            int next = i + Character.charCount(cp);
            if (!set[next] || positions[next] < positions[i]) {
                GlyphInfo info = getGlyph(cp);
                positions[next] = positions[i] + (info == null ? 0.0F : info.getAdvance());
            }
            for (int pos = i + 1; pos < next && pos < positions.length; pos++) {
                positions[pos] = positions[i];
            }
            i = next;
        }

        if (bold) {
            int glyphIndex = 0;
            for (int i = 0; i < len; ) {
                glyphIndex++;
                int cp = text.codePointAt(i);
                int next = i + Character.charCount(cp);
                for (int pos = i + 1; pos <= next && pos < positions.length; pos++) {
                    positions[pos] += glyphIndex;
                }
                i = next;
            }
        }
        return positions;
    }

    /**
     * Detects a truly fixed-width, independently shaped run for this derived font instance.
     * The decision is cached by a skeleton key so changing values (for example a debug line's
     * numbers) does not rebuild a full GlyphVector every time. A failed probe stays on the exact
     * shaping path.
     */
    @Nullable
    private float[] fixedCharacterPositions(String text, boolean bold) {
        if (text.length() < 2 || text.length() > 256 || !safeFixedText(text)) return null;
        FixedLayoutKey profileKey = new FixedLayoutKey(fixedSkeleton(text), bold);
        FixedLayoutProfile cached = fixedLayoutCache.get(profileKey);
        if (cached != null) {
            return cached.valid ? composeFixedPositions(text, bold, cached.advance) : null;
        }

        int codePoints = text.codePointCount(0, text.length());
        int offset = 0;
        float reference = Float.NaN;
        for (int i = 0; i < codePoints; i++) {
            int codePoint = text.codePointAt(offset);
            float advance = singleAdvance(codePoint);
            if (!Float.isFinite(advance) || advance <= 0.0F) {
                fixedLayoutCache.put(profileKey, FixedLayoutProfile.INVALID);
                return null;
            }
            if (i == 0) reference = advance;
            if (Math.abs(advance - reference) > 0.0001F) {
                fixedLayoutCache.put(profileKey, FixedLayoutProfile.INVALID);
                return null;
            }
            offset += Character.charCount(codePoint);
        }

        // Digits are the changing part of the cache key. Probe the whole ASCII digit set once
        // before reusing a profile for another value, so an accidentally proportional font cannot
        // be accepted merely because the first sample contained a subset of the digits.
        if (profileKey.containsDigits) {
            for (int digit = '0'; digit <= '9'; digit++) {
                float advance = singleAdvance(digit);
                if (!Float.isFinite(advance) || Math.abs(advance - reference) > 0.0001F) {
                    fixedLayoutCache.put(profileKey, FixedLayoutProfile.INVALID);
                    return null;
                }
            }
        }

        // A fixed advance alone is insufficient: reject ligatures, contextual substitutions,
        // fallback changes and kerning that alter the full GlyphVector's cluster boundaries.
        FontRenderContext frc = new FontRenderContext(null, antialias, fractionalMetrics);
        char[] chars = text.toCharArray();
        GlyphVector gv = font.layoutGlyphVector(frc, chars, 0, chars.length, Font.LAYOUT_LEFT_TO_RIGHT);
        if (gv.getNumGlyphs() != codePoints || glyphsAreNotOneToOne(gv, text)
                || glyphsAreContextual(gv, frc, text, reference)
                || Math.abs((float) gv.getGlyphPosition(gv.getNumGlyphs()).getX() / oversample
                - reference * codePoints) > 0.0001F) {
            fixedLayoutCache.put(profileKey, FixedLayoutProfile.INVALID);
            return null;
        }
        fixedLayoutCache.put(profileKey, new FixedLayoutProfile(true, reference));
        return composeFixedPositions(text, bold, reference);
    }

    private float singleAdvance(int codePoint) {
        String one = new String(Character.toChars(codePoint));
        // Measure the actual glyph advance without the synthetic bold offset. The complete run
        // applies that offset cumulatively after shaping, which composeFixedPositions reproduces.
        float[] single = layoutPositionsExact(one, false);
        return single[single.length - 1];
    }

    private float[] composeFixedPositions(String text, boolean bold, float advance) {
        float[] positions = new float[text.length() + 1];
        int offset = 0;
        float x = 0.0F;
        while (offset < text.length()) {
            int codePoint = text.codePointAt(offset);
            x += advance;
            int next = offset + Character.charCount(codePoint);
            for (int i = offset + 1; i <= next && i < positions.length; i++) positions[i] = x;
            offset = next;
        }
        if (bold) {
            int glyphIndex = 0;
            for (int i = 0; i < text.length(); ) {
                glyphIndex++;
                int next = i + Character.charCount(text.codePointAt(i));
                for (int p = i + 1; p <= next && p < positions.length; p++) positions[p] += glyphIndex;
                i = next;
            }
        }
        return positions;
    }

    private static boolean glyphsAreNotOneToOne(GlyphVector vector, String text) {
        int expected = 0;
        for (int glyph = 0; glyph < vector.getNumGlyphs(); glyph++) {
            int index = vector.getGlyphCharIndex(glyph);
            if (index != expected || index < 0 || index >= text.length()) return true;
            expected += Character.charCount(text.codePointAt(index));
        }
        return expected != text.length();
    }

    private boolean glyphsAreContextual(GlyphVector vector, FontRenderContext frc,
                                         String text, float advance) {
        int glyphIndex = 0;
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            String one = new String(Character.toChars(codePoint));
            GlyphVector single = font.createGlyphVector(frc, one);
            if (vector.getGlyphCode(glyphIndex) != single.getGlyphCode(0)
                    || Math.abs((float) vector.getGlyphPosition(glyphIndex).getX() / oversample
                    - advance * glyphIndex) > 0.0001F) {
                return true;
            }
            glyphIndex++;
            offset += Character.charCount(codePoint);
        }
        return false;
    }

    private static boolean safeFixedText(String text) {
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int type = Character.getType(cp);
            if ((cp >= 0xD800 && cp <= 0xDFFF) || Character.isISOControl(cp)
                    || type == Character.FORMAT || type == Character.NON_SPACING_MARK
                    || type == Character.COMBINING_SPACING_MARK || type == Character.ENCLOSING_MARK
                    || (cp >= 0x300 && cp <= 0x36F) || (cp >= 0x1AB0 && cp <= 0x1AFF)
                    || (cp >= 0x1DC0 && cp <= 0x1DFF) || (cp >= 0x20D0 && cp <= 0x20FF)
                    || (cp >= 0xFE00 && cp <= 0xFE0F) || (cp >= 0xE0100 && cp <= 0xE01EF)
                    || (cp >= 0x1F000 && cp <= 0x1FAFF) || (cp >= 0x2600 && cp <= 0x27BF)) {
                return false;
            }
            i += Character.charCount(cp);
        }
        return true;
    }

    private static String fixedSkeleton(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (cp >= '0' && cp <= '9') result.append('0');
            else result.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        return result.toString();
    }

    public int layoutCacheSize() {
        return layoutCache.size();
    }

    public int layoutCacheMax() {
        return MAX_LAYOUT_CACHE_SIZE;
    }

    public long layoutCacheHits() {
        return layoutCacheHits;
    }

    public long layoutCacheMisses() {
        return layoutCacheMisses;
    }

    public long layoutCacheEvictions() {
        return layoutCacheEvictions;
    }

    public int fixedLayoutProfileSize() {
        return fixedLayoutCache.size();
    }

    public void setFixedLayoutOptimization(boolean enabled) {
        fixedLayoutOptimization = enabled;
    }

    public boolean isFixedLayoutOptimization() {
        return fixedLayoutOptimization;
    }

    public void clearLayoutCaches() {
        layoutCache.clear();
        fixedLayoutCache.clear();
    }

    @Override
    public void close() {
        clearLayoutCaches();
    }

    private static final class LayoutKey {
        private final String text;
        private final boolean bold;

        private LayoutKey(String text, boolean bold) {
            this.text = text;
            this.bold = bold;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof LayoutKey)) {
                return false;
            }
            LayoutKey other = (LayoutKey) obj;
            return bold == other.bold && text.equals(other.text);
        }

        @Override
        public int hashCode() {
            return 31 * text.hashCode() + (bold ? 1 : 0);
        }
    }

    private static final class FixedLayoutKey {
        private final String skeleton;
        private final boolean bold;
        private final boolean containsDigits;

        private FixedLayoutKey(String skeleton, boolean bold) {
            this.skeleton = skeleton;
            this.bold = bold;
            this.containsDigits = skeleton.indexOf('0') >= 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof FixedLayoutKey other)) return false;
            return bold == other.bold && skeleton.equals(other.skeleton);
        }

        @Override
        public int hashCode() {
            return 31 * skeleton.hashCode() + (bold ? 1 : 0);
        }
    }

    private static final class FixedLayoutProfile {
        private static final FixedLayoutProfile INVALID = new FixedLayoutProfile(false, 0.0F);

        private final boolean valid;
        private final float advance;

        private FixedLayoutProfile(boolean valid, float advance) {
            this.valid = valid;
            this.advance = advance;
        }
    }

    private class AwtGlyphInfo implements GlyphInfo {

        private final int codePoint;
        private final float advance;
        private final float visualX;
        private final float visualY;
        private final float visualWidth;
        private final float visualHeight;
        private final float ascent;
        private final float glyphBaselineShift;

        AwtGlyphInfo(int codePoint) {
            this.codePoint = codePoint;
            String text = new String(Character.toChars(codePoint));

            FontRenderContext frc = new FontRenderContext(null, antialias, fractionalMetrics);
            GlyphVector gv = font.createGlyphVector(frc, text);
            GlyphMetrics metrics = gv.getGlyphMetrics(0);
            Rectangle2D visual = gv.getVisualBounds();
            LineMetrics lineMetrics = font.getLineMetrics(text, frc);

            this.advance = metrics.getAdvanceX() / oversample;
            this.visualX = (float) visual.getX();
            this.visualY = (float) visual.getY();
            this.visualWidth = (float) visual.getWidth();
            this.visualHeight = (float) visual.getHeight();
            this.ascent = lineMetrics.getAscent();
            this.glyphBaselineShift = (autoBaseline ? referenceBaseline - (ascent / oversample) : 0.0F)
                    + baselineShift;
        }

        @Override
        public float getAdvance() {
            return advance;
        }

        @Override
        public float getBoldOffset() {
            return 1.0F;
        }

        @Override
        public float getShadowOffset() {
            return 1.0F;
        }

        @Nullable
        @Override
        public BakedGlyph bake(FontTexture atlas) {
            String text = new String(Character.toChars(codePoint));

            int pad = 1;
            int minX = (int) Math.floor(Math.min(0.0F, visualX)) - pad;
            int maxX = (int) Math.ceil(Math.max(advance * oversample, visualX + visualWidth)) + pad;
            int minY = (int) Math.floor(visualY) - pad;
            int maxY = (int) Math.ceil(visualY + visualHeight) + pad;

            int rasterW = Math.max(1, maxX - minX);
            int rasterH = Math.max(1, maxY - minY);
            if (rasterW <= 0 || rasterH <= 0) {
                return atlas.add(new int[1], 1, 1,
                        0.0F, 0.0F, 0.0F, 0.0F,
                        oversample);
            }

            BufferedImage image = new BufferedImage(rasterW, rasterH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    antialiasHint);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            g2d.setFont(font);
            g2d.setColor(Color.WHITE);

            float drawX = shiftX - minX;
            float drawY = shiftY - minY;

            g2d.drawString(text, drawX, drawY);
            g2d.dispose();

            int[] pixels = image.getRGB(0, 0, rasterW, rasterH, null, 0, rasterW);
            FontPixelUtils.normalizeWhiteStraightAlpha(pixels);
            int textureScale = Math.round(FontRenderTuning.textureScale(oversample));
            if (textureScale > 1) {
                pixels = FontPixelUtils.scaleNearest(pixels, rasterW, rasterH, textureScale);
                rasterW *= textureScale;
                rasterH *= textureScale;
            }

            return atlas.add(pixels, rasterW, rasterH,
                    minX / oversample,
                    maxX / oversample,
                    (ascent + minY) / oversample + glyphBaselineShift,
                    (ascent + maxY) / oversample + glyphBaselineShift,
                    oversample);
        }
    }

    private static Object antialiasHint(boolean antialias, String antialiasMode) {
        if (!antialias) {
            return RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
        }
        String mode = antialiasMode == null ? "on" : antialiasMode.trim().toLowerCase().replace('-', '_');
        switch (mode) {
            case "off":
                return RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
            case "gasp":
                return RenderingHints.VALUE_TEXT_ANTIALIAS_GASP;
            case "lcd_hrgb":
                return RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB;
            case "lcd_hbgr":
                return RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR;
            case "lcd_vrgb":
                return RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB;
            case "lcd_vbgr":
                return RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR;
            default:
                return RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
        }
    }
}
