package neofontrender.core.font.preprocess;

import neofontrender.core.config.NeofontrenderConfig;

/**
 * Converts the laboratory {@code #RRGGBB} chat protocol into modern RGB runs.
 */
public final class HexChatTextPreprocessor implements RawTextPreprocessor {
    public static final HexChatTextPreprocessor INSTANCE = new HexChatTextPreprocessor();

    private HexChatTextPreprocessor() {
    }

    @Override
    public boolean enabled() {
        return NeofontrenderConfig.laboratoryHexChat();
    }

    @Override
    public boolean matches(String rawText) {
        if (rawText == null) return false;
        for (int index = 0; index + 6 < rawText.length(); index++) {
            if (isMarker(rawText, index)) return true;
        }
        return false;
    }

    @Override
    public PreprocessedText process(String rawText) {
        if (rawText == null || rawText.isEmpty() || !matches(rawText)) {
            return PreprocessedText.unchanged(rawText);
        }
        return LegacyColorTextParser.process(rawText, false, true);
    }

    static boolean isMarker(String text, int index) {
        return markerLength(text, index) > 0;
    }

    static int markerLength(String text, int index) {
        if (text == null || index < 0 || index >= text.length() || text.charAt(index) != '#'
                || index + 6 >= text.length()) return 0;
        int cursor = index + 1;
        while (cursor + 5 < text.length()) {
            if (!isHex6(text, cursor)) return cursor == index + 1 ? 0 : cursor - index;
            cursor += 6;
            if (cursor >= text.length() || text.charAt(cursor) != '-'
                    || !isHex6(text, cursor + 1)) break;
            cursor += 1;
        }
        return cursor == index + 1 ? 0 : cursor - index;
    }

    static int[] markerColors(String text, int index) {
        int length = markerLength(text, index);
        if (length == 0) return new int[0];
        String value = text.substring(index + 1, index + length);
        String[] parts = value.split("-");
        int[] colors = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            colors[i] = Integer.parseInt(parts[i], 16);
        }
        return colors;
    }

    static int gradientLength(String text, int index) {
        int length = 0;
        for (int cursor = index; cursor < text.length();) {
            if (isMarker(text, cursor)) break;
            if (text.charAt(cursor) == '\u00A7' && cursor + 1 < text.length()) {
                cursor += 2;
            } else {
                length++;
                cursor++;
            }
        }
        return length;
    }

    static int interpolate(int[] colors, int index, int totalLength) {
        if (colors.length == 0) return 0xFFFFFF;
        if (colors.length == 1 || totalLength <= 1) return colors[0];
        float position = (colors.length - 1) * Math.max(0, Math.min(totalLength - 1, index))
                / (float) (totalLength - 1);
        int before = Math.min(colors.length - 1, (int) position);
        int after = Math.min(colors.length - 1, before + 1);
        float fraction = position - before;
        return mix(colors[before] >> 16 & 255, colors[after] >> 16 & 255, fraction) << 16
                | mix(colors[before] >> 8 & 255, colors[after] >> 8 & 255, fraction) << 8
                | mix(colors[before] & 255, colors[after] & 255, fraction);
    }

    private static int mix(int before, int after, float fraction) {
        return Math.round(before * (1.0F - fraction) + after * fraction);
    }

    private static boolean isHex6(String text, int index) {
        if (index < 0 || index + 6 > text.length()) return false;
        for (int digit = index; digit < index + 6; digit++) {
            if (Character.digit(text.charAt(digit), 16) < 0) return false;
        }
        return true;
    }
}
