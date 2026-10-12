package neofontrender.addons.controller.sdl;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.input.InputValue;

import java.util.HashMap;
import java.util.Map;

/** Tracks button transitions locally because SDL exposes only the current button state. */
final class ControlStateTracker {
    private final Map<ResourceLocation, Boolean> previous = new HashMap<>();

    InputValue button(ResourceLocation control, boolean down) {
        boolean wasDown = previous.getOrDefault(control, false);
        previous.put(control, down);
        return InputValue.button(down, down && !wasDown, !down && wasDown);
    }

    void clear() {
        previous.clear();
    }
}
