package neofontrender.text;

/** Shared defaults for providers that rasterize structured inline content. */
public final class InlineRenderDefaults {
    private static volatile float rasterOversample = 2.0F;

    private InlineRenderDefaults() {}

    /** The laboratory controlled raster factor used by LaTeX and Typst providers. */
    public static float rasterOversample() {
        return rasterOversample;
    }

    public static void setRasterOversample(float value) {
        if (!Float.isFinite(value)) return;
        rasterOversample = Math.max(1.0F, Math.min(8.0F, value));
    }
}
