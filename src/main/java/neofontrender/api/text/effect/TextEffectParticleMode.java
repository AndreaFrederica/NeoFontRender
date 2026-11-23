package neofontrender.api.text.effect;

/** Optional particle side effect emitted by a captured text effect. */
public enum TextEffectParticleMode {
    /** The effect is fully represented by its glyph/shader output. */
    NONE,
    /** Read the legacy configurable particle parameters from the effect span. */
    CONFIGURED,
    /** Emit the built-in Brilliant flame particle simulation. */
    FLAME
}
