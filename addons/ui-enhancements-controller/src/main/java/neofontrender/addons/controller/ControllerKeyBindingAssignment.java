package neofontrender.addons.controller;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.input.InputValue;

import java.util.Objects;

/** One physical control assigned to a Minecraft KeyBinding, including an axis half-direction. */
final class ControllerKeyBindingAssignment {
    static final int ANY_DIRECTION = 0;
    static final int POSITIVE = 1;
    static final int NEGATIVE = -1;
    private static final float ACTIVATION_THRESHOLD = 0.55F;

    private final ResourceLocation control;
    private final int axisDirection;

    ControllerKeyBindingAssignment(ResourceLocation control, int axisDirection) {
        this.control = Objects.requireNonNull(control, "control");
        this.axisDirection = Integer.compare(axisDirection, 0);
    }

    ResourceLocation control() { return control; }
    int axisDirection() { return axisDirection; }

    boolean isDown(InputValue value) {
        if (value == null) return false;
        if (!ControllerControlCatalog.isAxis(control)) return value.isDown();
        if (axisDirection > 0) return value.getAxis() >= ACTIVATION_THRESHOLD;
        if (axisDirection < 0) return value.getAxis() <= -ACTIVATION_THRESHOLD;
        return Math.abs(value.getAxis()) >= ACTIVATION_THRESHOLD;
    }

    String directionSuffix() {
        return axisDirection > 0 ? " +" : axisDirection < 0 ? " -" : "";
    }
}
