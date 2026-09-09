package neofontrender.addons.textanimator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Shared anvil-name policy matching TextAnimator's server-side tag filtering. */
public final class TextAnimatorNamePolicy {
    public static final String ANVIL_NAMING_RULE = "textanimator:anvilNaming";
    private static final Set<String> EFFECTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "wave", "wiggle", "shake", "bounce", "swing", "pend", "turb", "fade",
            "pulse", "rainb", "grad", "shadow", "neon", "glitch", "scroll", "typewriter")));

    private TextAnimatorNamePolicy() {}

    public static String filter(String input, boolean allowAnimationTags) {
        if (allowAnimationTags || input == null || input.isEmpty()) return input;
        StringBuilder result = null;
        int copiedUntil = 0;
        int cursor = 0;
        while (cursor < input.length()) {
            int open = input.indexOf('<', cursor);
            if (open < 0) break;
            int close = input.indexOf('>', open + 1);
            if (close < 0) break;
            if (isKnownTag(input.substring(open + 1, close))) {
                if (result == null) result = new StringBuilder(input.length());
                result.append(input, copiedUntil, open);
                copiedUntil = close + 1;
            }
            cursor = close + 1;
        }
        if (result == null) return input;
        return result.append(input, copiedUntil, input.length()).toString();
    }

    private static boolean isKnownTag(String content) {
        String value = content.trim();
        if (value.isEmpty()) return false;
        if (value.charAt(0) == '/') value = value.substring(1).trim();
        int whitespace = firstWhitespace(value);
        String name = (whitespace < 0 ? value : value.substring(0, whitespace))
                .toLowerCase(Locale.ROOT);
        return EFFECTS.contains(name);
    }

    private static int firstWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) return index;
        }
        return -1;
    }
}
