package neofontrender.addons.camera;

/** Shoulder Surfing-compatible crosshair routing policy. */
enum ShoulderCrosshairType {
    ADAPTIVE,
    DYNAMIC,
    STATIC,
    STATIC_WITH_1PP,
    DYNAMIC_WITH_1PP;

    boolean dynamic(boolean aiming) {
        return this == DYNAMIC || this == DYNAMIC_WITH_1PP
                || (this == ADAPTIVE && aiming);
    }

    boolean switchesToFirstPerson(boolean aiming) {
        return aiming && (this == STATIC_WITH_1PP || this == DYNAMIC_WITH_1PP);
    }
}
