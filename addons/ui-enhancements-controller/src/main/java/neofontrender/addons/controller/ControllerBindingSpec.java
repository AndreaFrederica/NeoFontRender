package neofontrender.addons.controller;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.input.InputAction;

import java.util.Objects;

/** One editable physical-control slot for a logical UIE action. */
public final class ControllerBindingSpec {
    private final InputAction action;
    private final int slot;
    private final ResourceLocation control;
    private final float scale;
    private final boolean inverted;

    public ControllerBindingSpec(InputAction action, int slot, ResourceLocation control,
                                 float scale, boolean inverted) {
        this.action = Objects.requireNonNull(action, "action");
        this.slot = Math.max(0, slot);
        this.control = control;
        this.scale = Float.isFinite(scale) ? Math.max(0.0F, Math.min(4.0F, scale)) : 1.0F;
        this.inverted = inverted;
    }

    public InputAction getAction() { return action; }
    public int getSlot() { return slot; }
    public ResourceLocation getControl() { return control; }
    public float getScale() { return scale; }
    public boolean isInverted() { return inverted; }
    public boolean isBound() { return control != null; }

    public ControllerBindingSpec withControl(ResourceLocation value) {
        return new ControllerBindingSpec(action, slot, value, scale, inverted);
    }

    public String key() { return action.name() + ":" + slot; }
}
