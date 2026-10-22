package neofontrender.text.animation;

/** Controls whether an animated text span is rasterized as a run or as glyph instances. */
public enum TextAnimationRenderMode {
    /** Preserve shaping and modern text features whenever possible. */
    AUTO,
    /** Always keep the backend's native run/shaping path. */
    WHOLE_RUN,
    /** Request glyph transforms; unsafe shaped content is still downgraded to a run. */
    GLYPH
}
