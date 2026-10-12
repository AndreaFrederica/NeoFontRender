package neofontrender.addons.api.camera;

/** Immutable pixel-space bounds of projected world geometry. */
public final class ScreenBounds {
    private final double minimumX, minimumY, maximumX, maximumY;
    private final boolean visible;

    public ScreenBounds(double minimumX, double minimumY, double maximumX, double maximumY,
                        boolean visible) {
        this.minimumX = finite(Math.min(minimumX, maximumX));
        this.minimumY = finite(Math.min(minimumY, maximumY));
        this.maximumX = finite(Math.max(minimumX, maximumX));
        this.maximumY = finite(Math.max(minimumY, maximumY));
        this.visible = visible;
    }

    public static ScreenBounds invisible() {
        return new ScreenBounds(0.0D, 0.0D, 0.0D, 0.0D, false);
    }

    public double minimumX() { return minimumX; }
    public double minimumY() { return minimumY; }
    public double maximumX() { return maximumX; }
    public double maximumY() { return maximumY; }
    public double width() { return maximumX - minimumX; }
    public double height() { return maximumY - minimumY; }
    public boolean isVisible() { return visible; }

    private static double finite(double value) { return Double.isFinite(value) ? value : 0.0D; }
}
