package neofontrender.core.font.backend;

/**
 * Backend-independent soft-shadow composition used when a renderer cannot blur a raster itself.
 * Requested geometry is scaled with the caller-selected font size, just like native glyphs.
 */
public final class SampledShadowTextRenderResult implements TextRenderResult {
    private static final float[][] OFFSETS = {
            {0.0F, 0.0F},
            {-0.45F, 0.0F}, {0.45F, 0.0F}, {0.0F, -0.45F}, {0.0F, 0.45F},
            {-0.32F, -0.32F}, {0.32F, -0.32F}, {-0.32F, 0.32F}, {0.32F, 0.32F},
            {-0.85F, 0.0F}, {0.85F, 0.0F}, {0.0F, -0.85F}, {0.0F, 0.85F},
            {-0.60F, -0.60F}, {0.60F, -0.60F}, {-0.60F, 0.60F}, {0.60F, 0.60F}
    };

    private final TextRenderResult shadow;
    private final TextRenderResult foreground;
    private final float offsetX;
    private final float offsetY;
    private final float radius;
    private final float opacity;
    private final int samples;

    public SampledShadowTextRenderResult(TextRenderResult shadow, TextRenderResult foreground,
                                         float offsetX, float offsetY, float blurRadius,
                                         float opacity, float geometryScale) {
        this.shadow = shadow == null ? TextRenderResult.EMPTY : shadow;
        this.foreground = foreground == null ? TextRenderResult.EMPTY : foreground;
        float scale = Float.isFinite(geometryScale) ? Math.max(0.0F, geometryScale) : 1.0F;
        this.offsetX = offsetX * scale;
        this.offsetY = offsetY * scale;
        this.radius = Math.max(0.0F, blurRadius * scale);
        this.opacity = Math.max(0.0F, Math.min(1.0F, opacity));
        this.samples = radius < 0.05F ? 1 : radius <= 2.0F ? 9 : OFFSETS.length;
    }

    @Override public float advance() { return foreground.advance(); }

    @Override
    public float visualLeft() {
        return Math.min(foreground.visualLeft(), shadow.visualLeft() + offsetX - radius);
    }

    @Override
    public float visualRight() {
        return Math.max(foreground.visualRight(), shadow.visualRight() + offsetX + radius);
    }

    @Override
    public float visualTop() {
        return Math.min(foreground.visualTop(), shadow.visualTop() + offsetY - radius);
    }

    @Override
    public float visualBottom() {
        return Math.max(foreground.visualBottom(), shadow.visualBottom() + offsetY + radius);
    }

    @Override
    public void draw(float x, float y, float alpha) {
        float callerAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
        if (opacity > 0.0F) {
            // Preserve the requested total opacity where samples overlap instead of dividing it
            // into nearly invisible fragments.
            float sampleAlpha = samples == 1 ? opacity
                    : 1.0F - (float) Math.pow(1.0F - opacity, 1.0F / samples);
            for (int index = 0; index < samples; index++) {
                shadow.draw(x + offsetX + OFFSETS[index][0] * radius,
                        y + offsetY + OFFSETS[index][1] * radius,
                        callerAlpha * sampleAlpha);
            }
        }
        foreground.draw(x, y, callerAlpha);
    }
}
