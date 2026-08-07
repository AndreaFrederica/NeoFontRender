package neofontrender.addons.api.flight;

/** Pitch-ladder coverage policy for one flight-reference component. */
public enum FlightHudPitchMode {
    /** Draw only the authored local pitch interval, suitable for conventional airliner HUDs. */
    LIMITED,
    /** Populate the full spherical ladder and wrap it continuously through +/-180 degrees. */
    WRAP_360
}
