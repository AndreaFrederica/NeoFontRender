package neofontrender.typst.pipeline;

/** Parses bounded {@code <typst:...></typst>} tokens without interpreting Typst in Java. */
final class TypstTokenParser {
    private static final String PREFIX = "<typst:";
    private static final String SUFFIX = "</typst>";

    private TypstTokenParser() {}

    static Match match(CharSequence source, int start, int maximumLength) {
        if (source == null || start < 0 || start + PREFIX.length() >= source.length()
                || !startsWithIgnoreCase(source, start, PREFIX) || escaped(source, start)) {
            return null;
        }
        int contentStart = start + PREFIX.length();
        int limit = Math.min(source.length(), start + maximumLength
                + PREFIX.length() + SUFFIX.length());
        int longClose = indexOfIgnoreCase(source, SUFFIX, contentStart, limit);
        int nextOpening = indexOfIgnoreCase(source, PREFIX, contentStart, limit);
        if (longClose > contentStart && (nextOpening < 0 || longClose < nextOpening)) {
            String body = source.subSequence(contentStart, longClose).toString().trim();
            return body.isEmpty() || body.length() > maximumLength ? null
                    : new Match(start, longClose + SUFFIX.length(), body);
        }
        for (int index = contentStart; index < limit; index++) {
            if (source.charAt(index) != '>' || escaped(source, index)) continue;
            if (index == contentStart) return null;
            String body = source.subSequence(contentStart, index).toString().trim();
            return body.isEmpty() || body.length() > maximumLength ? null
                    : new Match(start, index + 1, body);
        }
        return null;
    }

    private static int indexOfIgnoreCase(CharSequence source, String value,
                                         int start, int limit) {
        int last = limit - value.length();
        for (int index = start; index <= last; index++) {
            boolean match = true;
            for (int offset = 0; offset < value.length(); offset++) {
                if (Character.toLowerCase(source.charAt(index + offset)) != value.charAt(offset)) {
                    match = false;
                    break;
                }
            }
            if (match) return index;
        }
        return -1;
    }

    private static boolean startsWithIgnoreCase(CharSequence source, int offset, String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.toLowerCase(source.charAt(offset + index)) != value.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private static boolean escaped(CharSequence source, int index) {
        int slashes = 0;
        for (int cursor = index - 1; cursor >= 0 && source.charAt(cursor) == '\\'; cursor--) {
            slashes++;
        }
        return (slashes & 1) != 0;
    }

    static final class Match {
        final int start;
        final int end;
        final String source;

        Match(int start, int end, String source) {
            this.start = start;
            this.end = end;
            this.source = source;
        }
    }
}
