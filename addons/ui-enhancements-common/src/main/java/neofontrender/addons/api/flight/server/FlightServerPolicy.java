package neofontrender.addons.api.flight.server;

/** Immutable per-player policy used by the optional UIE flight synchronization channel. */
public final class FlightServerPolicy {
    private final boolean enabled;
    private final boolean synchronizationEnabled;
    private final boolean requireElytra;
    private final float maximumRollSpeed;
    private final double synchronizationRange;

    public FlightServerPolicy(boolean enabled, boolean synchronizationEnabled,
                              boolean requireElytra, float maximumRollSpeed,
                              double synchronizationRange) {
        this.enabled = enabled;
        this.synchronizationEnabled = synchronizationEnabled;
        this.requireElytra = requireElytra;
        this.maximumRollSpeed = clamp(maximumRollSpeed, 0.0F, 720.0F, 180.0F);
        this.synchronizationRange = clamp(synchronizationRange, 0.0D, 4096.0D, 192.0D);
    }

    public boolean isEnabled() { return enabled; }
    public boolean isSynchronizationEnabled() { return synchronizationEnabled; }
    public boolean isElytraRequired() { return requireElytra; }
    public float getMaximumRollSpeed() { return maximumRollSpeed; }
    public double getSynchronizationRange() { return synchronizationRange; }
    public FlightServerPolicy withEnabled(boolean value) {
        return new FlightServerPolicy(value, synchronizationEnabled, requireElytra,
                maximumRollSpeed, synchronizationRange);
    }
    public FlightServerPolicy withSynchronization(boolean value) {
        return new FlightServerPolicy(enabled, value, requireElytra,
                maximumRollSpeed, synchronizationRange);
    }
    public FlightServerPolicy withElytraRequired(boolean value) {
        return new FlightServerPolicy(enabled, synchronizationEnabled, value,
                maximumRollSpeed, synchronizationRange);
    }
    public FlightServerPolicy withMaximumRollSpeed(float value) {
        return new FlightServerPolicy(enabled, synchronizationEnabled, requireElytra,
                value, synchronizationRange);
    }
    public FlightServerPolicy withSynchronizationRange(double value) {
        return new FlightServerPolicy(enabled, synchronizationEnabled, requireElytra,
                maximumRollSpeed, value);
    }

    private static float clamp(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
    private static double clamp(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
