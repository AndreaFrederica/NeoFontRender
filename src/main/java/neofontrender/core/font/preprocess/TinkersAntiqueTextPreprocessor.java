package neofontrender.core.font.preprocess;

import neofontrender.core.config.NeofontrenderConfig;

/**
 * Decodes Tinkers' Construct/TinkersAntique's three-character PUA RGB protocol.
 */
public final class TinkersAntiqueTextPreprocessor implements RawTextPreprocessor {
    public static final TinkersAntiqueTextPreprocessor INSTANCE =
            new TinkersAntiqueTextPreprocessor();

    public static final char MARKER_START = '\uE700';
    public static final char MARKER_END = '\uE7FF';

    private TinkersAntiqueTextPreprocessor() {
    }

    @Override
    public boolean enabled() {
        return NeofontrenderConfig.compatTinkersAntique();
    }

    @Override
    public boolean matches(String rawText) {
        if (rawText == null) return false;
        for (int i = 0; i < rawText.length(); i++) {
            if (isMarker(rawText.charAt(i))) return true;
        }
        return false;
    }

    @Override
    public PreprocessedText process(String rawText) {
        if (rawText == null || rawText.isEmpty() || !matches(rawText)) {
            return PreprocessedText.unchanged(rawText);
        }
        return LegacyColorTextParser.process(rawText, true, false);
    }

    public static boolean isMarker(char character) {
        return character >= MARKER_START && character <= MARKER_END;
    }

}
