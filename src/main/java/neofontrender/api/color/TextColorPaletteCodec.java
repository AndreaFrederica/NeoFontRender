package neofontrender.api.color;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Parses and formats user-editable legacy text color palettes. */
public final class TextColorPaletteCodec {
    private TextColorPaletteCodec() {
    }

    /**
     * Parses comma, semicolon, or whitespace-separated RRGGBB values. The first 16 values are the
     * normal colors. Supplying all 32 also overrides the shadow colors; otherwise shadows are
     * derived from the first 16. Missing or invalid foreground entries retain vanilla defaults.
     */
    public static int[] parse(String value) {
        int[] vanilla = TextColorPaletteRegistry.vanillaColorCodes();
        if (value == null || value.trim().isEmpty()) return vanilla;

        List<Integer> parsed = new ArrayList<>(32);
        for (String token : value.trim().split("[,;\\s]+")) {
            String hex = token.trim();
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.startsWith("0x") || hex.startsWith("0X")) hex = hex.substring(2);
            if (!hex.matches("(?i)[0-9a-f]{6}")) continue;
            parsed.add(Integer.parseInt(hex, 16));
            if (parsed.size() == 32) break;
        }

        int[] foreground = new int[16];
        System.arraycopy(vanilla, 0, foreground, 0, foreground.length);
        for (int index = 0; index < Math.min(16, parsed.size()); index++) {
            foreground[index] = parsed.get(index);
        }
        if (parsed.size() < 32) {
            return TextColorPaletteRegistry.normalizeColorCodes(foreground);
        }
        int[] result = new int[32];
        for (int index = 0; index < result.length; index++) result[index] = parsed.get(index);
        return TextColorPaletteRegistry.normalizeColorCodes(result);
    }

    /** Formats all 32 RGB entries as an editable comma-separated string. */
    public static String format(int[] colorCodes) {
        int[] normalized = TextColorPaletteRegistry.normalizeColorCodes(colorCodes);
        StringBuilder result = new StringBuilder(normalized.length * 7);
        for (int index = 0; index < normalized.length; index++) {
            if (index > 0) result.append(',');
            result.append(String.format(Locale.ROOT, "%06X", normalized[index]));
        }
        return result.toString();
    }
}
