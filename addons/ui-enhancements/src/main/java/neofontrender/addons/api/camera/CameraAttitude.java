package neofontrender.addons.api.camera;

/**
 * Immutable normalized quaternion. It maps camera-local axes to world axes: +Z forward, +Y up,
 * +X left, matching the UIE Flight coordinate convention.
 */
public final class CameraAttitude {
    public static final CameraAttitude IDENTITY = new CameraAttitude(0.0D, 0.0D, 0.0D, 1.0D);
    public final double x;
    public final double y;
    public final double z;
    public final double w;

    public CameraAttitude(double x, double y, double z, double w) {
        double length = Math.sqrt(x * x + y * y + z * z + w * w);
        if (!Double.isFinite(length) || length < 1.0E-12D) {
            this.x = this.y = this.z = 0.0D;
            this.w = 1.0D;
        } else {
            this.x = x / length; this.y = y / length;
            this.z = z / length; this.w = w / length;
        }
    }

    public static CameraAttitude axisAngle(CameraVector axis, double radians) {
        double length = axis.length();
        if (length < 1.0E-12D || !Double.isFinite(radians)) return IDENTITY;
        double half = radians * 0.5D;
        double scale = Math.sin(half) / length;
        return new CameraAttitude(axis.x * scale, axis.y * scale, axis.z * scale, Math.cos(half));
    }

    /** Builds an attitude from orthonormal camera-right/up/forward world vectors. */
    public static CameraAttitude fromBasis(CameraVector right, CameraVector up,
                                           CameraVector forward) {
        double m00 = -right.x, m01 = up.x, m02 = forward.x;
        double m10 = -right.y, m11 = up.y, m12 = forward.y;
        double m20 = -right.z, m21 = up.z, m22 = forward.z;
        double trace = m00 + m11 + m22;
        double x, y, z, w;
        if (trace > 0.0D) {
            double s = Math.sqrt(trace + 1.0D) * 2.0D;
            w = 0.25D * s; x = (m21 - m12) / s;
            y = (m02 - m20) / s; z = (m10 - m01) / s;
        } else if (m00 > m11 && m00 > m22) {
            double s = Math.sqrt(1.0D + m00 - m11 - m22) * 2.0D;
            w = (m21 - m12) / s; x = 0.25D * s;
            y = (m01 + m10) / s; z = (m02 + m20) / s;
        } else if (m11 > m22) {
            double s = Math.sqrt(1.0D + m11 - m00 - m22) * 2.0D;
            w = (m02 - m20) / s; x = (m01 + m10) / s;
            y = 0.25D * s; z = (m12 + m21) / s;
        } else {
            double s = Math.sqrt(1.0D + m22 - m00 - m11) * 2.0D;
            w = (m10 - m01) / s; x = (m02 + m20) / s;
            y = (m12 + m21) / s; z = 0.25D * s;
        }
        return new CameraAttitude(x, y, z, w);
    }

    /** Compatibility boundary only; runtime control remains quaternion-native. */
    public static CameraAttitude fromMinecraftDegrees(double pitch, double yaw, double roll) {
        CameraAttitude yawRotation = axisAngle(new CameraVector(0.0D, 1.0D, 0.0D), Math.toRadians(-yaw));
        CameraAttitude pitchRotation = axisAngle(new CameraVector(1.0D, 0.0D, 0.0D), Math.toRadians(pitch));
        CameraAttitude rollRotation = axisAngle(new CameraVector(0.0D, 0.0D, 1.0D), Math.toRadians(roll));
        return yawRotation.multiply(pitchRotation).multiply(rollRotation);
    }

    public CameraAttitude multiply(CameraAttitude local) {
        return new CameraAttitude(
                w * local.x + x * local.w + y * local.z - z * local.y,
                w * local.y - x * local.z + y * local.w + z * local.x,
                w * local.z + x * local.y - y * local.x + z * local.w,
                w * local.w - x * local.x - y * local.y - z * local.z);
    }

    public CameraAttitude conjugate() { return new CameraAttitude(-x, -y, -z, w); }

    public CameraVector rotate(CameraVector vector) {
        double tx = 2.0D * (y * vector.z - z * vector.y);
        double ty = 2.0D * (z * vector.x - x * vector.z);
        double tz = 2.0D * (x * vector.y - y * vector.x);
        return new CameraVector(vector.x + w * tx + (y * tz - z * ty),
                vector.y + w * ty + (z * tx - x * tz),
                vector.z + w * tz + (x * ty - y * tx));
    }

    public CameraVector forward() { return rotate(new CameraVector(0.0D, 0.0D, 1.0D)); }
    public CameraVector right() { return rotate(new CameraVector(-1.0D, 0.0D, 0.0D)); }
    public CameraVector up() { return rotate(new CameraVector(0.0D, 1.0D, 0.0D)); }

    public double angularDistance(CameraAttitude other) {
        double dot = Math.abs(x * other.x + y * other.y + z * other.z + w * other.w);
        return 2.0D * Math.acos(clamp(dot, -1.0D, 1.0D));
    }

    public CameraAttitude slerp(CameraAttitude target, double amount) {
        amount = clamp(amount, 0.0D, 1.0D);
        double dot = x * target.x + y * target.y + z * target.z + w * target.w;
        double tx = target.x, ty = target.y, tz = target.z, tw = target.w;
        if (dot < 0.0D) { dot = -dot; tx = -tx; ty = -ty; tz = -tz; tw = -tw; }
        if (dot > 0.9995D) return new CameraAttitude(x + (tx - x) * amount,
                y + (ty - y) * amount, z + (tz - z) * amount, w + (tw - w) * amount);
        double angle = Math.acos(clamp(dot, -1.0D, 1.0D));
        double sine = Math.sin(angle);
        double a = Math.sin((1.0D - amount) * angle) / sine;
        double b = Math.sin(amount * angle) / sine;
        return new CameraAttitude(x * a + tx * b, y * a + ty * b, z * a + tz * b, w * a + tw * b);
    }

    /** Selects the Euler branch nearest a caller-owned prior render boundary. */
    public CameraEulerAngles toMinecraftEuler(double referencePitch, double referenceYaw,
                                              double referenceRoll) {
        CameraVector forward = forward();
        double pitch = -Math.toDegrees(Math.asin(clamp(forward.y, -1.0D, 1.0D)));
        double horizontal = Math.sqrt(forward.x * forward.x + forward.z * forward.z);
        double yaw = horizontal < 1.0E-7D ? referenceYaw
                : Math.toDegrees(Math.atan2(-forward.x, forward.z));
        CameraAttitude zeroRoll = fromMinecraftDegrees(pitch, yaw, 0.0D);
        CameraVector zeroRight = zeroRoll.right();
        CameraVector actualRight = right();
        double roll = Math.toDegrees(Math.atan2(forward.dot(zeroRight.cross(actualRight)),
                clamp(zeroRight.dot(actualRight), -1.0D, 1.0D)));
        Candidate best = candidate(pitch, yaw, roll,
                referencePitch, referenceYaw, referenceRoll);
        double mirroredPitch = pitch >= 0.0D ? 180.0D - pitch : -180.0D - pitch;
        Candidate mirrored = candidate(mirroredPitch, yaw + 180.0D, roll + 180.0D,
                referencePitch, referenceYaw, referenceRoll);
        if (mirrored.cost < best.cost) best = mirrored;
        return new CameraEulerAngles(best.pitch, best.yaw, best.roll);
    }

    private static Candidate candidate(double pitch, double yaw, double roll,
                                       double referencePitch, double referenceYaw,
                                       double referenceRoll) {
        pitch += Math.rint((referencePitch - pitch) / 360.0D) * 360.0D;
        yaw += Math.rint((referenceYaw - yaw) / 360.0D) * 360.0D;
        roll += Math.rint((referenceRoll - roll) / 360.0D) * 360.0D;
        double dp = pitch - referencePitch, dy = yaw - referenceYaw, dr = roll - referenceRoll;
        return new Candidate(pitch, yaw, roll, dp * dp + dy * dy + dr * dr);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Candidate {
        final double pitch, yaw, roll, cost;
        Candidate(double pitch, double yaw, double roll, double cost) {
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
            this.cost = cost;
        }
    }
}
