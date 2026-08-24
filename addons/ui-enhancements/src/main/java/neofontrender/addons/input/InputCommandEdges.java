package neofontrender.addons.input;

import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputFrame;

import java.util.EnumMap;
import java.util.Map;

/** Generates consumer-rate button edges from routed held state. */
public final class InputCommandEdges {
    private final Map<InputAction, Boolean> previousDown = new EnumMap<>(InputAction.class);

    public boolean pressed(InputFrame frame, InputAction action) {
        boolean down = frame != null && frame.get(action).isDown();
        boolean previous = previousDown.getOrDefault(action, false);
        previousDown.put(action, down);
        return down && !previous;
    }

    public void clear() { previousDown.clear(); }
}
