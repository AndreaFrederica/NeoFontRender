package neofontrender.addons.flight;

/** Selects the opt-in event-cancellation policy for the current flight state. */
final class CrosshairEventPolicy {
    private CrosshairEventPolicy() {}

    static boolean shouldCancel(boolean flying) {
        return flying ? CrosshairConfig.cancelCrosshairEventDuringFlight
                : CrosshairConfig.cancelCrosshairEventOnGround;
    }
}
