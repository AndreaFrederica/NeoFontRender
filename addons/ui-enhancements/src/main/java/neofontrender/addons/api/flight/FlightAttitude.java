package neofontrender.addons.api.flight;

import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraEulerAngles;
import neofontrender.addons.api.camera.CameraVector;

/**
 * Flight API v9 compatibility value backed by the canonical camera quaternion implementation.
 * Local +Z is forward, +X is left wing and +Y is up.
 */
public final class FlightAttitude {
    public static final FlightAttitude IDENTITY = new FlightAttitude(0.0D, 0.0D, 0.0D, 1.0D);
    public final double x;
    public final double y;
    public final double z;
    public final double w;

    public FlightAttitude(double x, double y, double z, double w) {
        CameraAttitude normalized = new CameraAttitude(x, y, z, w);
        this.x = normalized.x;
        this.y = normalized.y;
        this.z = normalized.z;
        this.w = normalized.w;
    }

    /** Minecraft yaw/pitch plus right-wing-down roll. */
    public static FlightAttitude fromMinecraftDegrees(double pitch, double yaw, double roll) {
        return fromCamera(CameraAttitude.fromMinecraftDegrees(pitch, yaw, roll));
    }

    public static FlightAttitude axisAngle(double axisX, double axisY, double axisZ,
                                           double radians) {
        return fromCamera(CameraAttitude.axisAngle(
                new CameraVector(axisX, axisY, axisZ), radians));
    }

    /** Builds an attitude from orthonormal aircraft-right/up/forward world vectors. */
    public static FlightAttitude fromBasis(FlightVector right, FlightVector up,
                                           FlightVector forward) {
        return fromCamera(CameraAttitude.fromBasis(camera(right), camera(up), camera(forward)));
    }

    /** Composition: this world attitude followed by a rotation in aircraft-local coordinates. */
    public FlightAttitude rotateLocal(double axisX, double axisY, double axisZ, double radians) {
        return fromCamera(camera().multiply(CameraAttitude.axisAngle(
                new CameraVector(axisX, axisY, axisZ), radians)));
    }

    public FlightAttitude multiply(FlightAttitude other) {
        return fromCamera(camera().multiply(other.camera()));
    }

    public FlightAttitude conjugate() { return fromCamera(camera().conjugate()); }

    public FlightVector rotate(double vectorX, double vectorY, double vectorZ) {
        return flight(camera().rotate(new CameraVector(vectorX, vectorY, vectorZ)));
    }

    public FlightVector forward() { return flight(camera().forward()); }
    public FlightVector right() { return flight(camera().right()); }
    public FlightVector up() { return flight(camera().up()); }

    public double angularDistance(FlightAttitude other) {
        return camera().angularDistance(other.camera());
    }

    public FlightAttitude slerp(FlightAttitude target, double amount) {
        return fromCamera(camera().slerp(target.camera(), amount));
    }

    /** Selects the equivalent yaw/pitch/roll branch nearest the previous camera state. */
    public FlightEulerAngles toMinecraftEuler(double referencePitch, double referenceYaw,
                                               double referenceRoll) {
        CameraEulerAngles angles = camera().toMinecraftEuler(
                referencePitch, referenceYaw, referenceRoll);
        return new FlightEulerAngles(angles.pitchDegrees, angles.yawDegrees, angles.rollDegrees);
    }

    private CameraAttitude camera() { return new CameraAttitude(x, y, z, w); }
    private static FlightAttitude fromCamera(CameraAttitude value) {
        return new FlightAttitude(value.x, value.y, value.z, value.w);
    }
    private static CameraVector camera(FlightVector value) {
        return new CameraVector(value.x, value.y, value.z);
    }
    private static FlightVector flight(CameraVector value) {
        return new FlightVector(value.x, value.y, value.z);
    }
}
