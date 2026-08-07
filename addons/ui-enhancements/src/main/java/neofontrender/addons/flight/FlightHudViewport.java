package neofontrender.addons.flight;

import java.awt.Rectangle;

/** Maps a theme's virtual design canvas into a centered, aspect-safe game viewport. */
final class FlightHudViewport {
    private static final float SAFE_WIDTH = 0.94F;
    private static final float SAFE_HEIGHT = 0.88F;

    private FlightHudViewport() {}

    static Rectangle fit(int viewportWidth, int viewportHeight,
                         int canvasWidth, int canvasHeight, int scalePercent) {
        int safeViewportWidth = Math.max(1, Math.round(viewportWidth * SAFE_WIDTH));
        int safeViewportHeight = Math.max(1, Math.round(viewportHeight * SAFE_HEIGHT));
        float fit = Math.min(safeViewportWidth / (float) Math.max(1, canvasWidth),
                safeViewportHeight / (float) Math.max(1, canvasHeight));
        float userScale = Math.max(0.5F, Math.min(1.0F, scalePercent / 100.0F));
        float mappedScale = fit * userScale;
        int width = Math.max(1, Math.round(canvasWidth * mappedScale));
        int height = Math.max(1, Math.round(canvasHeight * mappedScale));
        return new Rectangle((viewportWidth - width) / 2,
                (viewportHeight - height) / 2, width, height);
    }
}
