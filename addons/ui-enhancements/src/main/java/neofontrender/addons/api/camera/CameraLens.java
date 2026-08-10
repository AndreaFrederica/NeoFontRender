package neofontrender.addons.api.camera;

/** Immutable viewport and projection parameters for one render sample. */
public final class CameraLens {
    private final int width, height;
    private final double verticalFovDegrees, nearPlane, farPlane;

    public CameraLens(int width, int height, double verticalFovDegrees,
                      double nearPlane, double farPlane) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.verticalFovDegrees = clamp(verticalFovDegrees, 1.0D, 179.0D, 70.0D);
        this.nearPlane = Math.max(1.0E-6D, finite(nearPlane, 0.05D));
        this.farPlane = Math.max(this.nearPlane, finite(farPlane, 1024.0D));
    }
    public int width() { return width; }
    public int height() { return height; }
    public double verticalFovDegrees() { return verticalFovDegrees; }
    public double aspectRatio() { return (double) width / height; }
    public double nearPlane() { return nearPlane; }
    public double farPlane() { return farPlane; }
    private static double finite(double value, double fallback) { return Double.isFinite(value) ? value : fallback; }
    private static double clamp(double value, double min, double max, double fallback) {
        return Math.max(min, Math.min(max, finite(value, fallback)));
    }
}
