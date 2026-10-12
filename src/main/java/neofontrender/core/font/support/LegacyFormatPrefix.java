package neofontrender.core.font.support;

/**
 * Extracts the legacy {@code §} formatting that stays in effect at the end of a string.
 *
 * <p>{@code FontRenderer#getFormatFromString(String)} answers this question on 1.12.2, but the
 * 1.7.10 renderer keeps that method private and the route wrappers cannot call it. This class
 * owns a local copy of the vanilla scan so both wrappers keep the original behaviour: a colour
 * code replaces the carried prefix, style codes accumulate in the order they appear, and every
 * other character is dropped.</p>
 */
public final class LegacyFormatPrefix {

    private static final String FORMAT_CODE = "\u00a7";

    private LegacyFormatPrefix() {}

    /**
     * Returns the formatting that has to be re-applied when text continues past the end of
     * {@code text}, or an empty string when no code is still in effect.
     */
    public static String of(String text) {
        String prefix = "";
        int length = text.length();
        int index = -1;
        while ((index = text.indexOf(FORMAT_CODE, index + 1)) != -1) {
            if (index + 1 >= length) break;
            char code = text.charAt(index + 1);
            if (isColorCode(code)) {
                prefix = FORMAT_CODE + code;
            } else if (isStyleCode(code)) {
                prefix = prefix + FORMAT_CODE + code;
            }
        }
        return prefix;
    }

    /** Colour codes take over the whole prefix instead of extending it. */
    private static boolean isColorCode(char code) {
        return code >= '0' && code <= '9'
                || code >= 'a' && code <= 'f'
                || code >= 'A' && code <= 'F';
    }

    /** Style codes stay ordered and are appended after a colour code. */
    private static boolean isStyleCode(char code) {
        return code >= 'k' && code <= 'o'
                || code >= 'K' && code <= 'O'
                || code == 'r' || code == 'R';
    }
}
