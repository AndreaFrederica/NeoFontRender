package neofontrender.core.font.skia;

import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.Typeface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Detects whether text would render as color glyphs by probing the system's
 * color-capable fonts (COLR/CBDT/SVG/sbix tables).
 *
 * <p>A run is classified "color" if any of its code points has a non-empty
 * glyph in a color-capable font. Color runs must keep baking color into the
 * texture; monochrome runs can use the white-glyph + runtime-tint fast path.
 *
 * <p>Detection is conservative: it only flags code points that a real color
 * font can render, rather than hard-coding emoji code-point ranges. This
 * catches symbols (e.g. U+2660) that a color emoji font paints in color,
 * which a pure range check would miss.
 */
public final class ColorGlyphDetector implements AutoCloseable {

    /** OpenType tables that carry color glyph data. */
    private static final String[] COLOR_TABLES = {"COLR", "CBDT", "SVG ", "sbix"};

    private static final int RUN_CACHE_LIMIT = 1024;

    private final Typeface[] colorTypefaces;
    private final Map<String, Boolean> runCache = Collections.synchronizedMap(
            new LinkedHashMap<String, Boolean>(64, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > RUN_CACHE_LIMIT;
                }
            });

    private ColorGlyphDetector(Typeface[] colorTypefaces) {
        this.colorTypefaces = colorTypefaces;
    }

    /**
     * Resolve color-capable typefaces for the given candidate font families
     * (typically the platform emoji fonts). Families that are absent, resolve
     * to a duplicate, or lack color tables are silently skipped.
     */
    public static ColorGlyphDetector create(String[] candidateColorFontFamilies) {
        if (candidateColorFontFamilies == null || candidateColorFontFamilies.length == 0) {
            return new ColorGlyphDetector(new Typeface[0]);
        }
        FontMgr fontMgr;
        try {
            fontMgr = FontMgr.getDefault();
        } catch (Throwable t) {
            return new ColorGlyphDetector(new Typeface[0]);
        }
        List<Typeface> collected = new ArrayList<>();
        Set<String> seenFamilies = new HashSet<>();
        for (String family : candidateColorFontFamilies) {
            if (family == null || family.trim().isEmpty()) {
                continue;
            }
            Typeface typeface;
            try {
                typeface = fontMgr.matchFamilyStyle(family, FontStyle.NORMAL);
            } catch (Throwable t) {
                continue;
            }
            if (typeface == null) {
                continue;
            }
            String resolvedFamily = null;
            try {
                resolvedFamily = typeface.getFamilyName();
            } catch (Throwable ignored) {
                // keep null
            }
            if (resolvedFamily != null && !seenFamilies.add(resolvedFamily)) {
                // same underlying family already collected via another alias
                typeface.close();
                continue;
            }
            if (!hasColorTable(typeface)) {
                typeface.close();
                continue;
            }
            collected.add(typeface);
        }
        return new ColorGlyphDetector(collected.toArray(new Typeface[0]));
    }

    private static boolean hasColorTable(Typeface typeface) {
        String[] tags;
        try {
            tags = typeface.getTableTags();
        } catch (Throwable t) {
            return false;
        }
        if (tags == null) {
            return false;
        }
        for (String tag : tags) {
            for (String colorTable : COLOR_TABLES) {
                if (colorTable.equals(tag)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** True if at least one color-capable font was found; if not, nothing is color. */
    public boolean hasColorFonts() {
        return colorTypefaces.length > 0;
    }

    /**
     * True if any color-capable font supplies a glyph for this code point.
     * Returns false immediately when no color fonts are available.
     */
    public boolean isColorCodePoint(int codePoint) {
        if (colorTypefaces.length == 0) {
            return false;
        }
        for (Typeface typeface : colorTypefaces) {
            try {
                if (typeface.getUTF32Glyph(codePoint) != 0) {
                    return true;
                }
            } catch (Throwable ignored) {
                // skip this typeface on failure
            }
        }
        return false;
    }

    /**
     * True if any code point in {@code text} renders as a color glyph.
     * Results are cached per text string.
     */
    public boolean isColorRun(String text) {
        if (text == null || text.isEmpty() || colorTypefaces.length == 0) {
            return false;
        }
        Boolean cached = runCache.get(text);
        if (cached != null) {
            return cached;
        }
        boolean color = false;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (isColorCodePoint(codePoint)) {
                color = true;
                break;
            }
            i += Character.charCount(codePoint);
        }
        runCache.put(text, color);
        return color;
    }

    @Override
    public void close() {
        for (Typeface typeface : colorTypefaces) {
            try {
                typeface.close();
            } catch (Throwable ignored) {
                // best effort during teardown
            }
        }
        runCache.clear();
    }
}
