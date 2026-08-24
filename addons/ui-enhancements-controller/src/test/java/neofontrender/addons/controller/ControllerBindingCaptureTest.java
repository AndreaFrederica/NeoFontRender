package neofontrender.addons.controller;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.input.InputValue;
import neofontrender.addons.controller.sdl.ControllerControls;
import neofontrender.addons.controller.sdl.ControllerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerBindingCaptureTest {
    @Test
    void capturesANewButtonEdge() {
        ControllerBindingCapture capture = new ControllerBindingCapture("test", snapshot(1L,
                ControllerControls.SOUTH, InputValue.button(false, false, false)));
        assertTrue(capture.isArmed());
        assertEquals(ControllerControls.SOUTH, capture.update(snapshot(2L,
                ControllerControls.SOUTH, InputValue.button(true, true, false))));
    }

    @Test
    void requiresHeldAxisToReturnToNeutralBeforeCapture() {
        ControllerBindingCapture capture = new ControllerBindingCapture("test", snapshot(1L,
                ControllerControls.LEFT_STICK_X, InputValue.axis(0.8F)));
        assertFalse(capture.isArmed());
        assertNull(capture.update(snapshot(2L,
                ControllerControls.LEFT_STICK_X, InputValue.axis(0.9F))));
        assertNull(capture.update(snapshot(3L,
                ControllerControls.LEFT_STICK_X, InputValue.axis(0.0F))));
        assertTrue(capture.isArmed());
        assertEquals(ControllerControls.LEFT_STICK_X, capture.update(snapshot(4L,
                ControllerControls.LEFT_STICK_X, InputValue.axis(-0.7F))));
    }

    @Test
    void capturesTheSelectedAxisHalfDirection() {
        ControllerBindingCapture capture = new ControllerBindingCapture("test", snapshot(1L,
                ControllerControls.LEFT_STICK_Y, InputValue.axis(0.0F)));

        ControllerBindingCapture.CapturedInput captured = capture.updateInput(snapshot(2L,
                ControllerControls.LEFT_STICK_Y, InputValue.axis(-0.8F)));

        assertEquals(ControllerControls.LEFT_STICK_Y, captured.control());
        assertEquals(ControllerKeyBindingAssignment.NEGATIVE, captured.axisDirection());
    }

    private static ControllerSnapshot snapshot(long time, ResourceLocation control,
                                               InputValue value) {
        Map<ResourceLocation, InputValue> controls = new LinkedHashMap<>();
        controls.put(control, value);
        return new ControllerSnapshot(new ResourceLocation("test", "device"),
                "Test controller", true, controls, time);
    }
}
