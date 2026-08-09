package neofontrender.addons.api.flight;

/** Immutable three-dimensional vector used by the public quaternion flight-attitude API. */
public final class FlightVector {
    public final double x;
    public final double y;
    public final double z;

    public FlightVector(double x, double y, double z) {
        this.x = finite(x); this.y = finite(y); this.z = finite(z);
    }

    public double lengthSquared() { return x * x + y * y + z * z; }
    public double dot(FlightVector other) { return x * other.x + y * other.y + z * other.z; }
    public FlightVector cross(FlightVector other) {
        return new FlightVector(y * other.z - z * other.y,
                z * other.x - x * other.z, x * other.y - y * other.x);
    }
    public FlightVector scale(double amount) {
        return new FlightVector(x * amount, y * amount, z * amount);
    }
    public FlightVector normalize() {
        double length = Math.sqrt(lengthSquared());
        return length < 1.0E-12D ? new FlightVector(0.0D, 0.0D, 0.0D)
                : new FlightVector(x / length, y / length, z / length);
    }

    private static double finite(double value) { return Double.isFinite(value) ? value : 0.0D; }
}
