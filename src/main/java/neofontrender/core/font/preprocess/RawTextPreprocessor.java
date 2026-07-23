package neofontrender.core.font.preprocess;

/**
 * Converts a legacy raw-string control protocol into modern colored text.
 */
public interface RawTextPreprocessor {
    boolean enabled();

    boolean matches(String rawText);

    PreprocessedText process(String rawText);
}
