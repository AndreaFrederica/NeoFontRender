package neofontrender.addons.api.camera;

/** A provider-selected world-space eye point. Coordinates must be finite as supplied. */
public final class CameraEyeAnchor {
    public final double x;
    public final double y;
    public final double z;

    public CameraEyeAnchor(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Camera eye anchor coordinates must be finite");
        }
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public CameraVector toCameraVector() { return new CameraVector(x, y, z); }
}
