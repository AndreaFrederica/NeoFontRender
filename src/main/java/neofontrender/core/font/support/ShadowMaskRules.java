package neofontrender.core.font.support;

import neofontrender.NeoFontRender;
import neofontrender.core.config.NeofontrenderConfig;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/** User-selected font and Unicode-range masks for text shadows. */
public final class ShadowMaskRules {
    private static volatile String lastFonts = "";
    private static volatile String lastCodepoints = "";
    private static volatile Font[] fonts = new Font[0];
    private static volatile int[][] ranges = new int[0][];

    private ShadowMaskRules() { }

    public static boolean matches(String text) {
        refresh();
        for (int offset = 0; text != null && offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            if (matchesCodePoint(codePoint) || matchesFont(codePoint)) return true;
            offset += Character.charCount(codePoint);
        }
        return false;
    }

    private static void refresh() {
        String configuredFonts = NeofontrenderConfig.shadowMaskFonts();
        String configuredCodepoints = NeofontrenderConfig.shadowMaskCodepoints();
        if (configuredFonts.equals(lastFonts) && configuredCodepoints.equals(lastCodepoints)) return;
        synchronized (ShadowMaskRules.class) {
            if (configuredFonts.equals(lastFonts) && configuredCodepoints.equals(lastCodepoints)) return;
            List<Font> parsedFonts = new ArrayList<>();
            for (String name : configuredFonts.split("[,;]")) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) parsedFonts.add(new Font(trimmed, Font.PLAIN, 12));
            }
            List<int[]> parsedRanges = new ArrayList<>();
            for (String value : configuredCodepoints.split("[,;\\s]+")) {
                String[] bounds = value.trim().replace("U+", "").split("-");
                try {
                    int start = Integer.parseInt(bounds[0], 16);
                    int end = bounds.length == 2 ? Integer.parseInt(bounds[1], 16) : start;
                    if (Character.isValidCodePoint(start) && Character.isValidCodePoint(end)) parsedRanges.add(new int[] {Math.min(start, end), Math.max(start, end)});
                } catch (NumberFormatException exception) {
                    NeoFontRender.LOGGER.warn("Ignoring invalid shadow codepoint range: {}", value, exception);
                }
            }
            fonts = parsedFonts.toArray(new Font[0]);
            ranges = parsedRanges.toArray(new int[0][]);
            lastFonts = configuredFonts;
            lastCodepoints = configuredCodepoints;
        }
    }

    private static boolean matchesCodePoint(int codePoint) {
        for (int[] range : ranges) if (codePoint >= range[0] && codePoint <= range[1]) return true;
        return false;
    }

    private static boolean matchesFont(int codePoint) {
        for (Font font : fonts) if (font.canDisplay(codePoint)) return true;
        return false;
    }
}
