package neofontrender.addons.input;

import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.input.InputFrame;
import neofontrender.addons.api.input.InputFrameContext;
import neofontrender.addons.api.input.InputValue;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputCommandEdgesTest {
    @Test void retainsOneCommandEdgeAcrossRepeatedConsumerTicks() {
        InputCommandEdges edges = new InputCommandEdges();
        InputFrame held = frame(1L, true);

        assertTrue(edges.pressed(held, InputAction.CAMERA_TOGGLE_DRONE));
        assertFalse(edges.pressed(held, InputAction.CAMERA_TOGGLE_DRONE));
        assertFalse(edges.pressed(frame(2L, false), InputAction.CAMERA_TOGGLE_DRONE));
        assertTrue(edges.pressed(frame(3L, true), InputAction.CAMERA_TOGGLE_DRONE));
    }

    private static InputFrame frame(long id, boolean down) {
        return InputApi.publish(new InputFrameContext(id, 0.0F, 0.05D, true),
                Collections.singletonMap(InputAction.CAMERA_TOGGLE_DRONE,
                        InputValue.button(down, false, false)));
    }
}
