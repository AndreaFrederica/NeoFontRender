package neofontrender.text.syntax;

/** A deterministic syntax rule provider invoked by the standard syntax engine. */
public interface TextSyntaxProvider {
    String id();
    int priority();
    char trigger();

    /** Allows protocols such as PUA byte ranges to share one registered provider. */
    default boolean acceptsTrigger(char value) { return value == trigger(); }

    /** Fallback rules run only after all ordinary providers fail to match. */
    default boolean isFallback() { return false; }

    SyntaxMatch match(SyntaxCursor cursor);
}
