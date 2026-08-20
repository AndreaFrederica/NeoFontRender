package neofontrender.addons.controller;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.input.InputValue;
import neofontrender.addons.controller.sdl.ControllerSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;

/** Noise-resistant state machine used by an embedded binding row. */
final class ControllerBindingCapture {
    private static final float RELEASE_THRESHOLD = 0.22F;
    private static final float CAPTURE_THRESHOLD = 0.35F;

    private final String bindingKey;
    private final Map<ResourceLocation, Float> previous = new LinkedHashMap<>();
    private boolean armed;

    ControllerBindingCapture(String bindingKey, ControllerSnapshot snapshot) {
        this.bindingKey = bindingKey;
        remember(snapshot);
        armed = neutral(snapshot);
    }

    String bindingKey() { return bindingKey; }
    boolean isArmed() { return armed; }

    ResourceLocation update(ControllerSnapshot snapshot) {
        CapturedInput captured = updateInput(snapshot);
        return captured == null ? null : captured.control();
    }

    CapturedInput updateInput(ControllerSnapshot snapshot) {
        if (snapshot == null || !snapshot.isConnected()) return null;
        if (!armed) {
            armed = neutral(snapshot);
            remember(snapshot);
            return null;
        }
        for (Map.Entry<ResourceLocation, InputValue> entry : snapshot.controls().entrySet()) {
            ResourceLocation control = entry.getKey();
            InputValue value = entry.getValue();
            if (!ControllerControlCatalog.isAxis(control) && value.isPressed()) {
                remember(snapshot);
                return new CapturedInput(control, ControllerKeyBindingAssignment.ANY_DIRECTION);
            }
        }
        ResourceLocation strongest = null;
        float strongestValue = CAPTURE_THRESHOLD;
        float signedValue = 0.0F;
        for (Map.Entry<ResourceLocation, InputValue> entry : snapshot.controls().entrySet()) {
            ResourceLocation control = entry.getKey();
            if (!ControllerControlCatalog.isAxis(control)) continue;
            float magnitude = Math.abs(entry.getValue().getAxis());
            float prior = Math.abs(previous.getOrDefault(control, 0.0F));
            if (prior < RELEASE_THRESHOLD && magnitude >= strongestValue) {
                strongest = control;
                strongestValue = magnitude;
                signedValue = entry.getValue().getAxis();
            }
        }
        remember(snapshot);
        return strongest == null ? null : new CapturedInput(strongest,
                signedValue < 0.0F ? ControllerKeyBindingAssignment.NEGATIVE
                        : ControllerKeyBindingAssignment.POSITIVE);
    }

    private static boolean neutral(ControllerSnapshot snapshot) {
        if (snapshot == null || !snapshot.isConnected()) return false;
        for (Map.Entry<ResourceLocation, InputValue> entry : snapshot.controls().entrySet()) {
            if (ControllerControlCatalog.isAxis(entry.getKey())) {
                if (Math.abs(entry.getValue().getAxis()) > RELEASE_THRESHOLD) return false;
            } else if (entry.getValue().isDown()) {
                return false;
            }
        }
        return true;
    }

    private void remember(ControllerSnapshot snapshot) {
        previous.clear();
        if (snapshot == null) return;
        snapshot.controls().forEach((control, value) -> previous.put(control, value.getAxis()));
    }

    static final class CapturedInput {
        private final ResourceLocation control;
        private final int axisDirection;

        CapturedInput(ResourceLocation control, int axisDirection) {
            this.control = control;
            this.axisDirection = axisDirection;
        }

        ResourceLocation control() { return control; }
        int axisDirection() { return axisDirection; }
    }
}
