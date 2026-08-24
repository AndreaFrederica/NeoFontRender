package neofontrender.addons.api.camera;

/** Screen-space world horizon derived from the complete camera attitude, including roll. */
public final class CameraHorizon {
    private final boolean visible;
    private final double startX, startY, endX, endY, angleDegrees;

    public CameraHorizon(boolean visible, double startX, double startY,
                         double endX, double endY, double angleDegrees) {
        this.visible = visible;
        this.startX = finite(startX);
        this.startY = finite(startY);
        this.endX = finite(endX);
        this.endY = finite(endY);
        this.angleDegrees = finite(angleDegrees);
    }

    public boolean isVisible() { return visible; }
    public double startX() { return startX; }
    public double startY() { return startY; }
    public double endX() { return endX; }
    public double endY() { return endY; }
    public double angleDegrees() { return angleDegrees; }

    private static double finite(double value) { return Double.isFinite(value) ? value : 0.0D; }
}
