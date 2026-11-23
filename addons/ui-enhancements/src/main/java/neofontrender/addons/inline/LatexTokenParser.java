package neofontrender.addons.inline;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small delimiter parser compatible with LaTeXNH's current $...$ syntax. */
final class LatexTokenParser {
    private static final int MAX_TOKEN = 2048;
    private static final Pattern SCALE_OPTION = Pattern.compile(
            "(?i)scale\\s*=\\s*([0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)");

    private LatexTokenParser() {}

    @Nullable static Match match(CharSequence source, int start) {
        if (start < 0 || start >= source.length() || source.charAt(start) != '$'
                || escaped(source, start)) return null;
        int delimiter = start + 1 < source.length() && source.charAt(start + 1) == '$' ? 2 : 1;
        int contentStart = start + delimiter;
        int limit = Math.min(source.length(), start + MAX_TOKEN);
        for (int index = contentStart; index < limit; index++) {
            if (source.charAt(index) != '$' || escaped(source, index)) continue;
            if (delimiter == 2 && (index + 1 >= limit || source.charAt(index + 1) != '$')) continue;
            if (index == contentStart) return null;
            int end = index + delimiter;
            float scale = 1.0F;
            if (end < limit && source.charAt(end) == '[') {
                int close = indexOf(source, ']', end + 1, limit);
                if (close > end) {
                    String option = source.subSequence(end + 1, close).toString().trim();
                    Matcher matcher = SCALE_OPTION.matcher(option);
                    if (matcher.matches()) {
                        try {
                            scale = Math.max(0.25F, Math.min(4.0F,
                                    Float.parseFloat(matcher.group(1))));
                            end = close + 1;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            return new Match(start, end, source.subSequence(contentStart, index).toString(),
                    delimiter == 2, scale);
        }
        return null;
    }

    private static boolean escaped(CharSequence source, int index) {
        int slashes = 0;
        for (int i = index - 1; i >= 0 && source.charAt(i) == '\\'; i--) slashes++;
        return (slashes & 1) != 0;
    }

    private static int indexOf(CharSequence source, char target, int start, int limit) {
        for (int i = start; i < limit; i++) if (source.charAt(i) == target) return i;
        return -1;
    }

    static final class Match {
        final int start;
        final int end;
        final String formula;
        final boolean display;
        final float scale;

        Match(int start, int end, String formula, boolean display, float scale) {
            this.start = start;
            this.end = end;
            this.formula = formula;
            this.display = display;
            this.scale = scale;
        }
    }
}
