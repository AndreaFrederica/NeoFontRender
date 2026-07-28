package neofontrender.addons.tooltips;

final class QuarkMapTooltipLayout {
    static final int PANEL_SIZE = QuarkMapTooltipCompat.CONTENT_SIZE
            + QuarkMapTooltipCompat.PANEL_PADDING * 2;
    private static final int SCREEN_MARGIN = 4;
    static final int PANEL_GAP = 3;

    private QuarkMapTooltipLayout() {}

    static Placement placeForTooltip(int screenWidth, int screenHeight, int tooltipContentX,
                                     int tooltipY, int tooltipContentWidth,
                                     int horizontalPadding, int verticalPadding) {
        int tooltipLeft = tooltipContentX - horizontalPadding;
        int tooltipTop = tooltipY - verticalPadding;
        int tooltipOuterWidth = tooltipContentWidth + horizontalPadding * 2;
        return place(screenWidth, screenHeight, tooltipLeft, tooltipTop, tooltipOuterWidth);
    }

    private static Placement place(int screenWidth, int screenHeight, int tooltipLeft,
                                   int tooltipTop, int tooltipOuterWidth) {
        int maxX = Math.max(SCREEN_MARGIN, screenWidth - PANEL_SIZE - SCREEN_MARGIN);
        int maxY = Math.max(SCREEN_MARGIN, screenHeight - PANEL_SIZE - SCREEN_MARGIN);
        int alignedX = clamp(tooltipLeft, SCREEN_MARGIN, maxX);
        int aboveY = tooltipTop - PANEL_SIZE - PANEL_GAP;
        if (aboveY >= SCREEN_MARGIN) return new Placement(alignedX, aboveY);

        int sideY = clamp(tooltipTop, SCREEN_MARGIN, maxY);
        int rightX = tooltipLeft + tooltipOuterWidth + PANEL_GAP;
        if (rightX <= maxX) return new Placement(rightX, sideY);

        int leftX = tooltipLeft - PANEL_SIZE - PANEL_GAP;
        if (leftX >= SCREEN_MARGIN) return new Placement(leftX, sideY);
        return new Placement(alignedX, SCREEN_MARGIN);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static final class Placement {
        final int x;
        final int y;

        Placement(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
