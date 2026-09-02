package neofontrender.addons.inline;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses explicit SVG references and their optional display-height suffix. */
final class SvgTokenParser {
    static final int DEFAULT_HEIGHT = 24;
    static final int MINIMUM_HEIGHT = 4;
    static final int MAXIMUM_HEIGHT = 256;
    private static final String PREFIX = "<svg:";
    private static final String PREFIX_WITHOUT_ANGLE = "svg:";
    private static final int MAX_TOKEN = 2048;
    private static final Pattern HEIGHT_OPTION = Pattern.compile(
            "(?i)height\\s*=\\s*([0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)");

    private SvgTokenParser() {}

    @Nullable static Match match(CharSequence source, int start) {
        int prefixLength;
        if (startsWith(source, start, PREFIX)) prefixLength = PREFIX.length();
        else if (startsWith(source, start, PREFIX_WITHOUT_ANGLE)) prefixLength = PREFIX_WITHOUT_ANGLE.length();
        else return null;
        int limit = Math.min(source.length(), start + MAX_TOKEN);
        int close = indexOf(source, '>', start + prefixLength, limit);
        if (close < 0) return null;
        String reference = source.subSequence(start + prefixLength, close).toString().trim();
        if (reference.isEmpty()) return null;

        int end = close + 1;
        int height = DEFAULT_HEIGHT;
        if (end < limit && source.charAt(end) == '[') {
            int optionEnd = indexOf(source, ']', end + 1, limit);
            if (optionEnd > end) {
                Matcher matcher = HEIGHT_OPTION.matcher(
                        source.subSequence(end + 1, optionEnd).toString().trim());
                if (matcher.matches()) {
                    try {
                        float requested = Float.parseFloat(matcher.group(1));
                        height = Math.max(MINIMUM_HEIGHT, Math.min(MAXIMUM_HEIGHT,
                                Math.round(requested)));
                        end = optionEnd + 1;
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return new Match(start, end, reference, height);
    }

    private static int indexOf(CharSequence source, char target, int start, int limit) {
        for (int index = start; index < limit; index++) if (source.charAt(index) == target) return index;
        return -1;
    }

    private static boolean startsWith(CharSequence source, int index, String value) {
        if (index < 0 || index + value.length() > source.length()) return false;
        for (int i = 0; i < value.length(); i++) {
            if (source.charAt(index + i) != value.charAt(i)) return false;
        }
        return true;
    }

    static final class Match {
        final int start;
        final int end;
        final String reference;
        final int height;

        Match(int start, int end, String reference, int height) {
            this.start = start;
            this.end = end;
            this.reference = reference;
            this.height = height;
        }
    }
}
