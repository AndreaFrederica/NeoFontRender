package neofontrender.api.text.effect;

import neofontrender.text.StructuredEffectSpan;

/** Generic visual definition for a structured text effect. */
public interface TextEffectDefinition {
    /** Stable namespaced effect id, for example {@code brilliant_text:g}. */
    String id();

    /** Shader family used by the built-in post-process renderer. */
    default float shaderType() { return 0.0F; }

    /** Classpath-relative shader resource paths. */
    default String vertexShader() { return "shaders/post/modern.vert"; }

    default String fragmentShader() { return "shaders/post/modern.frag"; }

    /** Allows an effect to normalize or add render parameters before drawing. */
    default StructuredEffectSpan prepare(StructuredEffectSpan effect) { return effect; }
}
