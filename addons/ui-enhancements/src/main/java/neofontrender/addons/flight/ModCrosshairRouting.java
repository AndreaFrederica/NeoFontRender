package neofontrender.addons.flight;

/** Pure policy for deciding when an item mod, rather than UIE, may render the active crosshair. */
final class ModCrosshairRouting {
    private ModCrosshairRouting() {}

    static boolean shouldOffset(boolean customEnabled, boolean preferModCrosshair) {
        return !customEnabled || preferModCrosshair;
    }

    static boolean shouldRenderFlightAim(boolean flightSuppressesCrosshair,
                                         boolean holdsPlayerAimItem, boolean visible) {
        return flightSuppressesCrosshair && holdsPlayerAimItem && visible;
    }
}
