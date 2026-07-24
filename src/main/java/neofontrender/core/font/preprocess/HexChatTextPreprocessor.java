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
        if (text.charAt(index) != '#' || index + 6 >= text.length()) return false;
        for (int digit = index + 1; digit <= index + 6; digit++) {
            if (Character.digit(text.charAt(digit), 16) < 0) return false;
        }
        return true;
    }
}
