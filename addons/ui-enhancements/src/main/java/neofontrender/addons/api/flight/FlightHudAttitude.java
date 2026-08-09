package neofontrender.addons.api.flight;

/** Quaternion aircraft attitude used by the conformal HUD. */
public final class FlightHudAttitude {
    private final FlightAttitude attitude;

    public FlightHudAttitude(FlightAttitude attitude) {
        this.attitude = java.util.Objects.requireNonNull(attitude, "attitude");
    }

    public FlightAttitude getAttitude() { return attitude; }
}
