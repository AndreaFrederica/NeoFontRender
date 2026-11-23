package neofontrender.text.syntax;

import neofontrender.text.animation.TextAnimationRenderMode;

/** A deterministic syntax rule provider invoked by the standard syntax engine. */
public interface TextSyntaxProvider {
    String id();
    int priority();
    char trigger();

    /** Dynamic provider gate. Disabled providers are invisible to parsing and provider listings. */
    default boolean isEnabled() { return true; }

    /**
     * Legacy provider-wide declaration. New providers must declare the mode on each
     * {@link EffectDescriptor}; this method is retained only for source compatibility.
     */
    @Deprecated
    default TextAnimationRenderMode animationRenderMode() {
        return TextAnimationRenderMode.WHOLE_RUN;
    }

    /** Allows protocols such as PUA byte ranges to share one registered provider. */
    default boolean acceptsTrigger(char value) { return value == trigger(); }

    /** Fallback rules run only after all ordinary providers fail to match. */
    default boolean isFallback() { return false; }

    SyntaxMatch match(SyntaxCursor cursor);
}
