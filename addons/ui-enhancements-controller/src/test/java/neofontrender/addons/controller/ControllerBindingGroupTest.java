package neofontrender.addons.controller;

import neofontrender.addons.api.input.InputAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerBindingGroupTest {
    @Test
    void classifiesEveryActionIntoOneSettingsGroup() {
        for (InputAction action : InputAction.values()) {
            ControllerBindingGroup group = ControllerBindingGroup.of(action);
            assertTrue(group.contains(action));
        }
        assertEquals(ControllerBindingGroup.PLAYER,
                ControllerBindingGroup.of(InputAction.PLAYER_ATTACK));
        assertEquals(ControllerBindingGroup.GUI,
                ControllerBindingGroup.of(InputAction.GUI_ACCEPT));
        assertEquals(ControllerBindingGroup.CAMERA,
                ControllerBindingGroup.of(InputAction.CAMERA_LOOK_X));
        assertEquals(ControllerBindingGroup.FLIGHT,
                ControllerBindingGroup.of(InputAction.FLIGHT_ROLL));
    }

    @Test
    void flightModeSeparatesAircraftAxesFromPlayerMovement() {
        assertTrue(ControllerInputMode.FLIGHT.accepts(InputAction.FLIGHT_PITCH));
        assertTrue(ControllerInputMode.FLIGHT.accepts(InputAction.CAMERA_LOOK_X));
        assertTrue(ControllerInputMode.FLIGHT.accepts(InputAction.PLAYER_ATTACK));
        assertFalse(ControllerInputMode.FLIGHT.accepts(InputAction.PLAYER_MOVE_FORWARD));
        assertFalse(ControllerInputMode.FLIGHT.accepts(InputAction.GUI_ACCEPT));
    }

    @Test
    void detachedCameraTakesPriorityOverFlightMode() {
        assertEquals(ControllerInputMode.CAMERA,
                ControllerInputMode.select(false, true, true));
        assertTrue(ControllerFlightCameraRuntime.shouldApply(true, false, false));
        assertFalse(ControllerFlightCameraRuntime.shouldApply(true, true, false));
        assertFalse(ControllerFlightCameraRuntime.shouldApply(true, false, true));
    }
}
