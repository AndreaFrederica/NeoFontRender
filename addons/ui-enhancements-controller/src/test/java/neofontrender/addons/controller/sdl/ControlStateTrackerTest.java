package neofontrender.addons.controller.sdl;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.input.InputValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlStateTrackerTest {
    @Test
    void emitsOnlyOnePressedAndReleasedEdgePerTransition() {
        ControlStateTracker tracker = new ControlStateTracker();
        ResourceLocation control = new ResourceLocation("test", "button");

        InputValue initial = tracker.button(control, false);
        InputValue pressed = tracker.button(control, true);
        InputValue held = tracker.button(control, true);
        InputValue released = tracker.button(control, false);

        assertFalse(initial.isPressed());
        assertTrue(pressed.isPressed());
        assertFalse(held.isPressed());
        assertTrue(released.isReleased());
    }
}
