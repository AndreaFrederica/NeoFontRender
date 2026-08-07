package neofontrender.addons.flight;

/** Pure three-axis camera-frame rotation used by the 1.12.2 flight controller. */
final class FlightOrientationMath {
    private static final Vec3 WORLD_UP = new Vec3(0.0D, 1.0D, 0.0D);

    private FlightOrientationMath() {}

    /** Forge 1.12 CameraSetup uses OpenGL view yaw: Minecraft entity yaw plus 180 degrees. */
    static float cameraEventYaw(float entityYaw) { return entityYaw + 180.0F; }

    static Orientation rotate(float pitch, float yaw, float roll,
                              double localPitchDegrees, double localYawDegrees,
                              double localRollDegrees) {
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad)).normalize();
        Vec3 levelLeft = levelLeft(forward, yawRad);
        Vec3 left = levelLeft.rotate(forward, Math.toRadians(roll)).normalize();

        // Positive Minecraft pitch points down, hence the negative local-axis rotation.
        forward = forward.rotate(left, -Math.toRadians(localPitchDegrees)).normalize();

        Vec3 localDown = forward.cross(left).normalize();
        double yawAmount = Math.toRadians(localYawDegrees);
        forward = forward.rotate(localDown, yawAmount).normalize();
        left = left.rotate(localDown, yawAmount).normalize();

        left = left.rotate(forward, Math.toRadians(localRollDegrees)).normalize();

        double newPitch = -Math.asin(clampUnit(forward.y));
        double newYaw = -Math.atan2(forward.x, forward.z);
        Vec3 newLevelLeft = levelLeft(forward, newYaw);
        double newRoll = -Math.atan2(left.cross(newLevelLeft).dot(forward),
                left.dot(newLevelLeft));
        return new Orientation((float) Math.toDegrees(newPitch),
                unwrapNear((float) Math.toDegrees(newYaw), yaw),
                unwrapNear((float) Math.toDegrees(newRoll), roll + (float) localRollDegrees));
    }

    private static Vec3 levelLeft(Vec3 forward, double yawRad) {
        Vec3 left = forward.cross(WORLD_UP);
        if (left.lengthSquared() < 1.0E-10D) {
            left = new Vec3(-Math.cos(yawRad), 0.0D, -Math.sin(yawRad));
        }
        return left.normalize();
    }

    private static float unwrapNear(float value, float reference) {
        float result = value;
        while (result - reference >= 180.0F) result -= 360.0F;
        while (result - reference < -180.0F) result += 360.0F;
        return result;
    }

    private static double clampUnit(double value) {
        return Math.max(-1.0D, Math.min(1.0D, value));
    }

    static final class Orientation {
        final float pitch;
        final float yaw;
        final float roll;

        private Orientation(float pitch, float yaw, float roll) {
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
        }
    }

    private static final class Vec3 {
        final double x;
        final double y;
        final double z;

        private Vec3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private Vec3 cross(Vec3 other) {
            return new Vec3(y * other.z - z * other.y,
                    z * other.x - x * other.z,
                    x * other.y - y * other.x);
        }

        private double dot(Vec3 other) {
            return x * other.x + y * other.y + z * other.z;
        }

        private double lengthSquared() {
            return dot(this);
        }

        private Vec3 normalize() {
            double length = Math.sqrt(lengthSquared());
            return length < 1.0E-12D ? this : new Vec3(x / length, y / length, z / length);
        }

        private Vec3 rotate(Vec3 axisValue, double angle) {
            Vec3 axis = axisValue.normalize();
            double cosine = Math.cos(angle);
            double sine = Math.sin(angle);
            Vec3 cross = axis.cross(this);
            double projection = axis.dot(this) * (1.0D - cosine);
            return new Vec3(x * cosine + cross.x * sine + axis.x * projection,
                    y * cosine + cross.y * sine + axis.y * projection,
                    z * cosine + cross.z * sine + axis.z * projection);
        }
    }
}
