package neofontrender.addons.api.flight;

/** Camera target driven by physical aircraft attitude instead of by the maneuver input source. */
public final class FlightCameraTracking {
    private final FlightAttitude attitude;
    private final float responsePerSecond;
    private final float maximumRateDegreesPerSecond;

    public FlightCameraTracking(FlightAttitude attitude, float responsePerSecond,
                                float maximumRateDegreesPerSecond) {
        this.attitude = java.util.Objects.requireNonNull(attitude, "attitude");
        this.responsePerSecond = positive(responsePerSecond);
        this.maximumRateDegreesPerSecond = positive(maximumRateDegreesPerSecond);
    }

    /** Tick-locked cockpit camera; Minecraft interpolation still smooths between physics ticks. */
    public static FlightCameraTracking rigid(FlightAttitude attitude) {
        return new FlightCameraTracking(attitude, Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY);
    }

    public FlightAttitude getAttitude() { return attitude; }
    public float getResponsePerSecond() { return responsePerSecond; }
    public float getMaximumRateDegreesPerSecond() { return maximumRateDegreesPerSecond; }
    public boolean isRigid() {
        return Float.isInfinite(responsePerSecond)
                || Float.isInfinite(maximumRateDegreesPerSecond);
    }

    private static float positive(float value) {
        if (Float.isInfinite(value) && value > 0.0F) return value;
        return Float.isFinite(value) ? Math.max(0.0F, value) : 0.0F;
    }
}
