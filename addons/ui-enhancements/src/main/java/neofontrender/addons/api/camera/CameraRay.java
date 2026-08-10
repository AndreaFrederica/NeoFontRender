package neofontrender.addons.api.camera;

/** Finite world-space ray emitted from an authoritative camera frame. */
public final class CameraRay {
    private final CameraVector origin;
    private final CameraVector direction;

    public CameraRay(CameraVector origin, CameraVector direction) {
        this.origin = origin == null ? new CameraVector(0.0D, 0.0D, 0.0D) : origin;
        this.direction = normalizedDirection(direction);
    }

    public CameraVector origin() { return origin; }
    public CameraVector direction() { return direction; }
    public CameraVector point(double distance) { return origin.add(direction.scale(Math.max(0.0D, distance))); }

    static CameraVector normalizedDirection(CameraVector value) {
        CameraVector normalized = value == null ? null : value.normalize();
        return normalized == null || normalized.lengthSquared() < 1.0E-12D
                ? new CameraVector(0.0D, 0.0D, 1.0D) : normalized;
    }
}
