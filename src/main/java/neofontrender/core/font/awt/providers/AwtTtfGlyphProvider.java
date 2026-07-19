package neofontrender.core.font.awt.providers;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
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
    private final Map<LayoutKey, float[]> layoutCache = Collections.synchronizedMap(
            new LinkedHashMap<LayoutKey, float[]>(MAX_LAYOUT_CACHE_SIZE, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<LayoutKey, float[]> eldest) {
                    if (size() > MAX_LAYOUT_CACHE_SIZE) {
                        layoutCacheEvictions++;
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
                referenceBaseline, antialias, antialias ? "on" : "off", fractionalMetrics, style, true);
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
                                           boolean allowDefaultFallback) throws IOException {
        Font baseFont;
        if (fontName != null && !fontName.isEmpty()) {
            File fontFile = new File(fontName);
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

    public float[] layoutPositions(String text, boolean bold) {
        LayoutKey key = new LayoutKey(text, bold);
        float[] cached = layoutCache.get(key);
        if (cached != null) {
            layoutCacheHits++;
            return cached;
        }
        layoutCacheMisses++;

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
        layoutCache.put(key, positions);
        return positions;
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
