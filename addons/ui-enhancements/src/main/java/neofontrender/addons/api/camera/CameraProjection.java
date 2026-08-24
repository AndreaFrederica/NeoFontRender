package neofontrender.addons.api.camera;

/** Structured world-to-screen result; invisible and invalid are deliberately distinct. */
public final class CameraProjection {
    public enum Visibility {
        VISIBLE,
        BEHIND_CAMERA,
        OUTSIDE_DEPTH_RANGE,
        OUTSIDE_VIEWPORT,
        INVALID
    }

    private final Visibility visibility;
    private final double pixelX;
    private final double pixelY;
    private final double depth;

    public CameraProjection(Visibility visibility, double pixelX, double pixelY, double depth) {
        this.visibility = visibility == null ? Visibility.INVALID : visibility;
        this.pixelX = finite(pixelX); this.pixelY = finite(pixelY); this.depth = finite(depth);
    }

    public Visibility visibility() { return visibility; }
    public double pixelX() { return pixelX; }
    public double pixelY() { return pixelY; }
    public double depth() { return depth; }

    private static double finite(double value) { return Double.isFinite(value) ? value : 0.0D; }
}
