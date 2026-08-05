package neofontrender.addons.api.flight;

/** Immutable public snapshot of UIE's local flight controller. Angles are degrees. */
public final class FlightState {
    public static final FlightState INACTIVE = new FlightState(false, 0, 0, 0, 0, 0, false);

    private final boolean active;
    private final float pitch;
    private final float yaw;
    private final float roll;
    private final float inputX;
    private final float inputY;
    private final boolean barrelRolling;
    private final boolean serverCompanionPresent;
    private final boolean serverAllowsControl;
    private final boolean remoteSynchronizationEnabled;
    private final float effectiveMaximumRollSpeed;

    public FlightState(boolean active, float pitch, float yaw, float roll,
                       float inputX, float inputY, boolean barrelRolling) {
        this(active, pitch, yaw, roll, inputX, inputY, barrelRolling,
                false, true, false, 180.0F);
    }

    public FlightState(boolean active, float pitch, float yaw, float roll,
                       float inputX, float inputY, boolean barrelRolling,
                       boolean serverCompanionPresent, boolean serverAllowsControl,
                       boolean remoteSynchronizationEnabled, float effectiveMaximumRollSpeed) {
        this.active = active;
        this.pitch = finite(pitch);
        this.yaw = finite(yaw);
        this.roll = finite(roll);
        this.inputX = axis(inputX);
        this.inputY = axis(inputY);
        this.barrelRolling = barrelRolling;
        this.serverCompanionPresent = serverCompanionPresent;
        this.serverAllowsControl = serverAllowsControl;
        this.remoteSynchronizationEnabled = remoteSynchronizationEnabled;
        this.effectiveMaximumRollSpeed = Math.max(0.0F, finite(effectiveMaximumRollSpeed));
    }

    public boolean isActive() { return active; }
    public float getPitch() { return pitch; }
    public float getYaw() { return yaw; }
    public float getRoll() { return roll; }
    public float getInputX() { return inputX; }
    public float getInputY() { return inputY; }
    public boolean isBarrelRolling() { return barrelRolling; }
    public boolean isServerCompanionPresent() { return serverCompanionPresent; }
    public boolean doesServerAllowControl() { return serverAllowsControl; }
    public boolean isRemoteSynchronizationEnabled() { return remoteSynchronizationEnabled; }
    public float getEffectiveMaximumRollSpeed() { return effectiveMaximumRollSpeed; }

    private static float finite(float value) { return Float.isFinite(value) ? value : 0.0F; }
    private static float axis(float value) { return Math.max(-1.0F, Math.min(1.0F, finite(value))); }
}
