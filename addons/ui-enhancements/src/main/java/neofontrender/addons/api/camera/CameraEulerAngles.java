package neofontrender.addons.api.camera;

/** Euler compatibility representation used only at Minecraft and Forge render boundaries. */
public final class CameraEulerAngles {
    public final float pitchDegrees;
    public final float yawDegrees;
    public final float rollDegrees;

    public CameraEulerAngles(double pitchDegrees, double yawDegrees, double rollDegrees) {
        this.pitchDegrees = finite(pitchDegrees);
        this.yawDegrees = finite(yawDegrees);
        this.rollDegrees = finite(rollDegrees);
    }

    private static float finite(double value) { return Double.isFinite(value) ? (float) value : 0.0F; }
}
