package neofontrender.addons.api.flight;

/** Independently overridable parts of UIE's flight stack. */
public enum FlightCapability {
    CONTROL,
    /** Built-in A/D yaw input, independently suppressible by custom flight models. */
    KEYBOARD_YAW,
    /** Legacy path that applies maneuver input directly to the player/camera orientation. */
    CAMERA_ROTATION,
    PLAYER_ROLL_RENDERING,
    HUD,
    CROSSHAIR_SUPPRESSION
}
