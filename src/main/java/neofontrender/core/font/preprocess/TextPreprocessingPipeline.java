package neofontrender.core.font.preprocess;

/**
 * Entry point for legacy raw-text compatibility middleware.
 */
public final class TextPreprocessingPipeline {
    private static final RawTextPreprocessor[] PREPROCESSORS = {
            TinkersAntiqueTextPreprocessor.INSTANCE
    };

    private TextPreprocessingPipeline() {
    }

    public static PreprocessedText process(String rawText) {
        for (RawTextPreprocessor preprocessor : PREPROCESSORS) {
            if (preprocessor.enabled() && preprocessor.matches(rawText)) {
                return preprocessor.process(rawText);
            }
        }
        return PreprocessedText.unchanged(rawText);
    }

    public static boolean isInvisibleControlCharacter(char character) {
        return TinkersAntiqueTextPreprocessor.INSTANCE.enabled()
                && TinkersAntiqueTextPreprocessor.isMarker(character);
    }
}
