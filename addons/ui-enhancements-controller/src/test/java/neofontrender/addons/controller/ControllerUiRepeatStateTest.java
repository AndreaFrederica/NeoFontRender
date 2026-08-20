package neofontrender.addons.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerUiRepeatStateTest {
    @Test void emitsAnImmediatePulseThenDelayedStableRepeats() {
        ControllerUiRepeatState state = new ControllerUiRepeatState();
        assertTrue(state.pulse(ControllerUiRepeatState.Pulse.DOWN, true));
        for (int tick = 2; tick < 10; tick++) {
            assertFalse(state.pulse(ControllerUiRepeatState.Pulse.DOWN, true));
        }
        assertTrue(state.pulse(ControllerUiRepeatState.Pulse.DOWN, true));
        assertFalse(state.pulse(ControllerUiRepeatState.Pulse.DOWN, true));
        assertFalse(state.pulse(ControllerUiRepeatState.Pulse.DOWN, true));
        assertTrue(state.pulse(ControllerUiRepeatState.Pulse.DOWN, true));
    }

    @Test void releasingResetsTheInitialEdge() {
        ControllerUiRepeatState state = new ControllerUiRepeatState();
        assertTrue(state.pulse(ControllerUiRepeatState.Pulse.RIGHT, true));
        assertFalse(state.pulse(ControllerUiRepeatState.Pulse.RIGHT, false));
        assertTrue(state.pulse(ControllerUiRepeatState.Pulse.RIGHT, true));
    }
}
