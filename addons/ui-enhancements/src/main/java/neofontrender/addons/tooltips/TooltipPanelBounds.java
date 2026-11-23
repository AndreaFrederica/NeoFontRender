package neofontrender.addons.tooltips;

/** The single application point for content-to-panel insets. */
final class TooltipPanelBounds {
    final int left, top, right, bottom;

    private TooltipPanelBounds(int left, int top, int right, int bottom) {
        this.left = left; this.top = top; this.right = right; this.bottom = bottom;
    }

    static TooltipPanelBounds of(int x, int y, int width, int visualTop, int visualBottom) {
        return new TooltipPanelBounds(x - TooltipConfig.leftPadding,
                y + visualTop - TooltipConfig.topPadding,
                x + width + TooltipConfig.rightPadding,
                y + visualBottom + TooltipConfig.bottomPadding);
    }

    int width() { return right - left; }
    int height() { return bottom - top; }
}
