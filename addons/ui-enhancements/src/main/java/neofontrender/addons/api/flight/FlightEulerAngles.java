package neofontrender.addons.api.flight;

/** Continuous Minecraft camera representation selected from a quaternion attitude. */
public final class FlightEulerAngles {
    public final float pitchDegrees;
    public final float yawDegrees;
    public final float rollDegrees;

    FlightEulerAngles(double pitch, double yaw, double roll) {
        pitchDegrees = (float) pitch;
        yawDegrees = (float) yaw;
        rollDegrees = (float) roll;
    }
}
