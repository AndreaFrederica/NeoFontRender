package neofontrender.core.font.preprocess;

/**
 * Entry point for legacy raw-text compatibility middleware.
 */
public final class TextPreprocessingPipeline {
    private TextPreprocessingPipeline() {
    }

    public static PreprocessedText process(String rawText) {
        boolean decodeTinkers = TinkersAntiqueTextPreprocessor.INSTANCE.enabled()
                && TinkersAntiqueTextPreprocessor.INSTANCE.matches(rawText);
        boolean decodeHex = HexChatTextPreprocessor.INSTANCE.enabled()
                && HexChatTextPreprocessor.INSTANCE.matches(rawText);
        return decodeTinkers || decodeHex
                ? LegacyColorTextParser.process(rawText, decodeTinkers, decodeHex)
                : PreprocessedText.unchanged(rawText);
    }

    public static boolean isInvisibleControlCharacter(char character) {
        return TinkersAntiqueTextPreprocessor.INSTANCE.enabled()
                && TinkersAntiqueTextPreprocessor.isMarker(character);
    }
}
