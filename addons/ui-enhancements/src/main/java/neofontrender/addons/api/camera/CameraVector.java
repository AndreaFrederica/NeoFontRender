package neofontrender.addons.api.camera;

/** Immutable finite world/local vector shared by camera measurement and control APIs. */
public final class CameraVector {
    public final double x;
    public final double y;
    public final double z;

    public CameraVector(double x, double y, double z) {
        this.x = finite(x);
        this.y = finite(y);
        this.z = finite(z);
    }

    public CameraVector add(CameraVector other) { return new CameraVector(x + other.x, y + other.y, z + other.z); }
    public CameraVector subtract(CameraVector other) { return new CameraVector(x - other.x, y - other.y, z - other.z); }
    public CameraVector scale(double amount) { return new CameraVector(x * amount, y * amount, z * amount); }
    public double dot(CameraVector other) { return x * other.x + y * other.y + z * other.z; }
    public CameraVector cross(CameraVector other) {
        return new CameraVector(y * other.z - z * other.y,
                z * other.x - x * other.z, x * other.y - y * other.x);
    }
    public double lengthSquared() { return dot(this); }
    public double length() { return Math.sqrt(lengthSquared()); }
    public CameraVector normalize() {
        double length = length();
        return length < 1.0E-12D ? new CameraVector(0.0D, 0.0D, 0.0D) : scale(1.0D / length);
    }

    private static double finite(double value) { return Double.isFinite(value) ? value : 0.0D; }
}
