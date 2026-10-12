package neofontrender.addons.api.camera;

/** Immutable swept camera collision query. */
public final class CameraCollisionQuery {
    private final CameraVector from, to;
    private final double radius;
    public CameraCollisionQuery(CameraVector from, CameraVector to, double radius) {
        this.from = from == null ? new CameraVector(0, 0, 0) : from;
        this.to = to == null ? this.from : to;
        this.radius = Math.max(0.0D, Double.isFinite(radius) ? radius : 0.0D);
    }
    public CameraVector from() { return from; }
    public CameraVector to() { return to; }
    public double radius() { return radius; }
}
