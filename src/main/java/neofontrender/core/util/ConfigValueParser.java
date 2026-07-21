package neofontrender.core.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Validation and serialization helpers shared by configuration consumers. */
public final class ConfigValueParser {
    private ConfigValueParser() {
    }

    public static float parseFloat(String value, float fallback, float min, float max) {
        try {
            float parsed = Float.parseFloat(value);
            if (Float.isNaN(parsed) || Float.isInfinite(parsed)) return fallback;
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static int parseInt(String value, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static List<String> parseFontList(String value) {
        Set<String> fonts = new LinkedHashSet<>();
        if (value != null) {
            for (String part : value.split("[,;]")) {
                String font = part.trim();
                if (!font.isEmpty()) fonts.add(font);
            }
        }
        return new ArrayList<>(fonts);
    }

    public static String joinFontList(List<String> fonts) {
        return fonts == null || fonts.isEmpty() ? "" : String.join(", ", fonts);
    }
}
