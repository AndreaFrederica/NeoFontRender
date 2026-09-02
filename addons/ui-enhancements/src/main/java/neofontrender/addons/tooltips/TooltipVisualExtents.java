package neofontrender.addons.tooltips;

/** Logical GUI-pixel area painted outside a tooltip's panel rectangle. */
final class TooltipVisualExtents {
    final int left;
    final int top;
    final int right;
    final int bottom;

    private TooltipVisualExtents(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    static TooltipVisualExtents current() {
        return calculate(TooltipConfig.borderWidth, TooltipConfig.antialiasWidth,
                TooltipConfig.shadowRadius, TooltipConfig.shadowAlpha,
                TooltipConfig.shadowOffsetX, TooltipConfig.shadowOffsetY);
    }

    static TooltipVisualExtents calculate(float borderWidth, float antialiasWidth,
                                           float shadowRadius, int shadowAlpha,
                                           float shadowOffsetX, float shadowOffsetY) {
        float edge = Math.max(0.0F, borderWidth) * 0.5F
                + Math.max(0.0F, antialiasWidth);
        float shadow = shadowAlpha <= 0 ? 0.0F : Math.max(0.0F, shadowRadius) + edge;
        return new TooltipVisualExtents(
                ceil(Math.max(edge, shadow - shadowOffsetX)),
                ceil(Math.max(edge, shadow - shadowOffsetY)),
                ceil(Math.max(edge, shadow + shadowOffsetX)),
                ceil(Math.max(edge, shadow + shadowOffsetY)));
    }

    private static int ceil(float value) {
        return Math.max(0, (int) Math.ceil(value));
    }
}
