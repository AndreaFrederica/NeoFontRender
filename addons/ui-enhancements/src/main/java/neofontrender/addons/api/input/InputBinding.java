package neofontrender.addons.api.input;

import net.minecraft.util.ResourceLocation;

import java.util.Objects;

/** Maps one raw device control to a normalized logical action. */
public final class InputBinding {
    private final ResourceLocation control;
    private final InputAction action;
    private final float deadzone;
    private final float scale;
    private final boolean inverted;

    public InputBinding(ResourceLocation control, InputAction action) {
        this(control, action, 0.0F, 1.0F, false);
    }

    public InputBinding(ResourceLocation control, InputAction action, float deadzone,
                        float scale, boolean inverted) {
        this.control = Objects.requireNonNull(control, "control");
        this.action = Objects.requireNonNull(action, "action");
        this.deadzone = Math.max(0.0F, Math.min(0.99F,
                Float.isFinite(deadzone) ? deadzone : 0.0F));
        this.scale = Math.max(0.0F, Float.isFinite(scale) ? scale : 1.0F);
        this.inverted = inverted;
    }

    public ResourceLocation getControl() { return control; }
    public InputAction getAction() { return action; }

    InputValue map(InputValue raw) {
        float magnitude = Math.abs(raw.getAxis());
        float axis = magnitude <= deadzone ? 0.0F
                : Math.copySign((magnitude - deadzone) / (1.0F - deadzone), raw.getAxis());
        axis *= scale;
        if (inverted) axis = -axis;
        return new InputValue(axis, raw.isDown(), raw.isPressed(), raw.isReleased());
    }
}
