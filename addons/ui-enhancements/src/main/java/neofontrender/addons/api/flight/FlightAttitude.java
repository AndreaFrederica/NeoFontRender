package neofontrender.addons.api.flight;

/**
 * Immutable normalized quaternion mapping aircraft-local axes to world axes.
 * Local +Z is forward/thrust, +X is left wing and +Y is aircraft up.  The right-wing vector is
 * local -X; this keeps the aircraft frame right-handed in Minecraft's +X east/+Y up/+Z south
 * world coordinates.
 */
public final class FlightAttitude {
    public static final FlightAttitude IDENTITY = new FlightAttitude(0.0D, 0.0D, 0.0D, 1.0D);
    public final double x;
    public final double y;
    public final double z;
    public final double w;

    public FlightAttitude(double x, double y, double z, double w) {
        double length = Math.sqrt(x * x + y * y + z * z + w * w);
        if (!Double.isFinite(length) || length < 1.0E-12D) {
            this.x = this.y = this.z = 0.0D; this.w = 1.0D;
        } else {
            this.x = x / length; this.y = y / length;
            this.z = z / length; this.w = w / length;
        }
    }

    /** Minecraft yaw/pitch plus right-wing-down roll. */
    public static FlightAttitude fromMinecraftDegrees(double pitch, double yaw, double roll) {
        FlightAttitude yawRotation = axisAngle(0.0D, 1.0D, 0.0D, Math.toRadians(-yaw));
        FlightAttitude pitchRotation = axisAngle(1.0D, 0.0D, 0.0D, Math.toRadians(pitch));
        FlightAttitude rollRotation = axisAngle(0.0D, 0.0D, 1.0D, Math.toRadians(roll));
        return yawRotation.multiply(pitchRotation).multiply(rollRotation);
    }

    public static FlightAttitude axisAngle(double axisX, double axisY, double axisZ,
                                           double radians) {
        double length = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        if (!Double.isFinite(length) || length < 1.0E-12D || !Double.isFinite(radians))
            return IDENTITY;
        double half = radians * 0.5D;
        double scale = Math.sin(half) / length;
        return new FlightAttitude(axisX * scale, axisY * scale, axisZ * scale, Math.cos(half));
    }

    /** Builds an attitude from orthonormal aircraft-right/up/forward world vectors. */
    public static FlightAttitude fromBasis(FlightVector right, FlightVector up,
                                           FlightVector forward) {
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
        return new FlightAttitude(x, y, z, w);
    }

    /** Composition: this world attitude followed by a rotation in aircraft-local coordinates. */
    public FlightAttitude rotateLocal(double axisX, double axisY, double axisZ, double radians) {
        return multiply(axisAngle(axisX, axisY, axisZ, radians));
    }

    public FlightAttitude multiply(FlightAttitude other) {
        return new FlightAttitude(
                w * other.x + x * other.w + y * other.z - z * other.y,
                w * other.y - x * other.z + y * other.w + z * other.x,
                w * other.z + x * other.y - y * other.x + z * other.w,
                w * other.w - x * other.x - y * other.y - z * other.z);
    }

    public FlightAttitude conjugate() { return new FlightAttitude(-x, -y, -z, w); }

    public FlightVector rotate(double vectorX, double vectorY, double vectorZ) {
        // Unit-quaternion vector rotation without allocating intermediate quaternions.
        double tx = 2.0D * (y * vectorZ - z * vectorY);
        double ty = 2.0D * (z * vectorX - x * vectorZ);
        double tz = 2.0D * (x * vectorY - y * vectorX);
        return new FlightVector(vectorX + w * tx + (y * tz - z * ty),
                vectorY + w * ty + (z * tx - x * tz),
                vectorZ + w * tz + (x * ty - y * tx));
    }

    public FlightVector forward() { return rotate(0.0D, 0.0D, 1.0D); }
    public FlightVector right() { return rotate(-1.0D, 0.0D, 0.0D); }
    public FlightVector up() { return rotate(0.0D, 1.0D, 0.0D); }

    public double angularDistance(FlightAttitude other) {
        double dot = Math.abs(x * other.x + y * other.y + z * other.z + w * other.w);
        return 2.0D * Math.acos(clamp(dot, -1.0D, 1.0D));
    }

    public FlightAttitude slerp(FlightAttitude target, double amount) {
        amount = clamp(amount, 0.0D, 1.0D);
        double dot = x * target.x + y * target.y + z * target.z + w * target.w;
        double tx = target.x, ty = target.y, tz = target.z, tw = target.w;
        if (dot < 0.0D) { dot = -dot; tx = -tx; ty = -ty; tz = -tz; tw = -tw; }
        if (dot > 0.9995D) return new FlightAttitude(x + (tx - x) * amount,
                y + (ty - y) * amount, z + (tz - z) * amount, w + (tw - w) * amount);
        double angle = Math.acos(clamp(dot, -1.0D, 1.0D));
        double sine = Math.sin(angle);
        double a = Math.sin((1.0D - amount) * angle) / sine;
        double b = Math.sin(amount * angle) / sine;
        return new FlightAttitude(x * a + tx * b, y * a + ty * b,
                z * a + tz * b, w * a + tw * b);
    }

    /** Selects the equivalent yaw/pitch/roll branch nearest the previous camera state. */
    public FlightEulerAngles toMinecraftEuler(double referencePitch, double referenceYaw,
                                               double referenceRoll) {
        FlightVector forward = forward();
        double pitch = -Math.toDegrees(Math.asin(clamp(forward.y, -1.0D, 1.0D)));
        double horizontal = Math.sqrt(forward.x * forward.x + forward.z * forward.z);
        double yaw = horizontal < 1.0E-7D ? referenceYaw
                : Math.toDegrees(Math.atan2(-forward.x, forward.z));
        FlightAttitude zeroRoll = fromMinecraftDegrees(pitch, yaw, 0.0D);
        FlightVector zeroRight = zeroRoll.right();
        FlightVector actualRight = right();
        double signed = Math.atan2(forward.dot(zeroRight.cross(actualRight)),
                clamp(zeroRight.dot(actualRight), -1.0D, 1.0D));
        double roll = Math.toDegrees(signed);

        Candidate best = candidate(pitch, yaw, roll,
                referencePitch, referenceYaw, referenceRoll);
        double mirroredPitch = pitch >= 0.0D ? 180.0D - pitch : -180.0D - pitch;
        Candidate mirrored = candidate(mirroredPitch, yaw + 180.0D, roll + 180.0D,
                referencePitch, referenceYaw, referenceRoll);
        if (mirrored.cost < best.cost) best = mirrored;
        return new FlightEulerAngles(best.pitch, best.yaw, best.roll);
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

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static final class Candidate {
        final double pitch, yaw, roll, cost;
        Candidate(double pitch, double yaw, double roll, double cost) {
            this.pitch = pitch; this.yaw = yaw; this.roll = roll; this.cost = cost;
        }
    }
}
