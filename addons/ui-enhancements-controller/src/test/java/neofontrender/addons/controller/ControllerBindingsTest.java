package neofontrender.addons.controller;

import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.controller.sdl.ControllerControls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerBindingsTest {
    @AfterEach
    void restore() {
        ControllerBindings.resetDefaults();
        ControllerConfig.setDeadzone(ControllerConfig.DEFAULT_DEADZONE);
        ControllerConfig.setLookSensitivity(ControllerConfig.DEFAULT_LOOK_SENSITIVITY);
        ControllerConfig.setInvertLookX(false);
    }

    @Test
    void containsAnEditableSlotForEveryLogicalAction() {
        EnumSet<InputAction> actions = EnumSet.noneOf(InputAction.class);
        for (ControllerBindingSpec spec : ControllerBindings.all()) actions.add(spec.getAction());
        assertEquals(EnumSet.allOf(InputAction.class), actions);
    }

    @Test
    void serializationPreservesAssignmentsAndUnboundSlots() {
        String zoom = InputAction.CAMERA_ZOOM.name() + ":0";
        String jump = InputAction.PLAYER_JUMP.name() + ":0";
        ControllerBindings.assign(zoom, ControllerControls.LEFT_TRIGGER);
        ControllerBindings.assign(jump, null);
        List<String> records = ControllerBindings.serialize();

        ControllerBindings.resetDefaults();
        ControllerBindings.load(records);

        assertEquals(ControllerControls.LEFT_TRIGGER, find(zoom).getControl());
        assertTrue(!find(jump).isBound());
    }

    @Test
    void previewExposesRawDeadzoneAndActionMappingStages() {
        ControllerConfig.setDeadzone(0.2F);
        ControllerConfig.setLookSensitivity(2.0F);
        assertEquals(0.0F, ControllerBindings.preview(
                ControllerControls.RIGHT_STICK_X, 0.1F), 1.0E-6F);
        assertEquals(0.5F, ControllerBindings.preview(
                ControllerControls.RIGHT_STICK_X, 0.4F), 1.0E-6F);
        ControllerConfig.setInvertLookX(true);
        assertEquals(-0.5F, ControllerBindings.preview(
                ControllerControls.RIGHT_STICK_X, 0.4F), 1.0E-6F);
    }

    @Test
    void defaultsUseSignedLeftStickAxesForPlayerMovement() {
        ControllerBindingSpec forward = find(InputAction.PLAYER_MOVE_FORWARD.name() + ":0");
        ControllerBindingSpec strafe = find(InputAction.PLAYER_MOVE_STRAFE.name() + ":0");
        assertEquals(ControllerControls.LEFT_STICK_Y, forward.getControl());
        assertTrue(forward.isInverted());
        assertEquals(ControllerControls.LEFT_STICK_X, strafe.getControl());
        assertTrue(strafe.isInverted());
    }

    @Test
    void defaultsExposeACompleteGuiControlLayer() {
        assertEquals(ControllerControls.LEFT_STICK_X,
                find(InputAction.GUI_CURSOR_X.name() + ":0").getControl());
        assertEquals(ControllerControls.LEFT_STICK_Y,
                find(InputAction.GUI_CURSOR_Y.name() + ":0").getControl());
        assertEquals(ControllerControls.RIGHT_STICK_Y,
                find(InputAction.GUI_SCROLL_Y.name() + ":0").getControl());
        assertEquals(ControllerControls.SOUTH,
                find(InputAction.GUI_ACCEPT.name() + ":0").getControl());
        assertEquals(ControllerControls.EAST,
                find(InputAction.GUI_BACK.name() + ":0").getControl());
        assertEquals(ControllerControls.NORTH,
                find(InputAction.GUI_QUICK_MOVE.name() + ":0").getControl());
        assertEquals(ControllerControls.LEFT_TRIGGER,
                find(InputAction.GUI_PAGE_PREVIOUS.name() + ":0").getControl());
        assertEquals(ControllerControls.RIGHT_TRIGGER,
                find(InputAction.GUI_PAGE_NEXT.name() + ":0").getControl());
    }

    @Test
    void defaultsUseMsfsStyleFlightStickAndPedals() {
        assertEquals(ControllerControls.LEFT_STICK_Y,
                find(InputAction.FLIGHT_PITCH.name() + ":0").getControl());
        assertEquals(ControllerControls.LEFT_STICK_X,
                find(InputAction.FLIGHT_ROLL.name() + ":0").getControl());
        assertEquals(ControllerControls.TRIGGER_RUDDER,
                find(InputAction.FLIGHT_RUDDER.name() + ":0").getControl());
    }

    private static ControllerBindingSpec find(String key) {
        return ControllerBindings.all().stream()
                .filter(spec -> spec.key().equals(key))
                .findFirst().orElseThrow();
    }
}
