package neofontrender.core.font.preprocess;

/**
 * Entry point for legacy raw-text compatibility middleware.
 */
public final class TextPreprocessingPipeline {
    private TextPreprocessingPipeline() {
    }

    public static PreprocessedText process(String rawText) {
        boolean decodeHex = HexChatTextPreprocessor.INSTANCE.enabled()
                && HexChatTextPreprocessor.INSTANCE.matches(rawText);
        return decodeHex
                ? LegacyColorTextParser.process(rawText)
                : PreprocessedText.unchanged(rawText);
    }
}
