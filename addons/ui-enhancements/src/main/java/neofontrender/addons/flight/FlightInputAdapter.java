package neofontrender.addons.flight;

import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.input.InputFrame;

/**
 * Staged bridge from the unified input API to the legacy Flight mouse event. A non-zero physical
 * mouse sample remains authoritative; virtual devices fill only a neutral mouse frame.
 */
final class FlightInputAdapter {
    private FlightInputAdapter() {}

    static float pitch(float rawDeltaY) {
        if (rawDeltaY != 0.0F) return 0.0F;
        return InputApi.getFrame(0.0F).get(InputAction.FLIGHT_PITCH).getAxis();
    }

    static float roll(float rawDeltaX) {
        if (rawDeltaX != 0.0F) return 0.0F;
        return InputApi.getFrame(0.0F).get(InputAction.FLIGHT_ROLL).getAxis();
    }

    static float yaw() {
        InputFrame frame = InputApi.getFrame(0.0F);
        return frame.get(InputAction.FLIGHT_YAW).getAxis();
    }

    static boolean isNeutral() {
        InputFrame frame = InputApi.getFrame(0.0F);
        return frame.get(InputAction.FLIGHT_PITCH).getAxis() == 0.0F
                && frame.get(InputAction.FLIGHT_YAW).getAxis() == 0.0F
                && frame.get(InputAction.FLIGHT_ROLL).getAxis() == 0.0F
                && frame.get(InputAction.FLIGHT_RUDDER).getAxis() == 0.0F;
    }
}
