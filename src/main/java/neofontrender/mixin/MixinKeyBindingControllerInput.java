package neofontrender.mixin;

import net.minecraft.client.settings.KeyBinding;
import neofontrender.api.client.input.NfrKeyBindingControllerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds per-instance controller state without changing or colliding with keyboard key codes. */
@Mixin(KeyBinding.class)
public abstract class MixinKeyBindingControllerInput implements NfrKeyBindingControllerInput {
    @Unique private boolean nfr$controllerDown;
    @Unique private int nfr$controllerPresses;

    @Override
    public void nfr$setControllerInput(boolean down, boolean pressed) {
        nfr$controllerDown = down;
        if (pressed && nfr$controllerPresses < 8) nfr$controllerPresses++;
    }

    @Override
    public void nfr$clearControllerInput() {
        nfr$controllerDown = false;
        nfr$controllerPresses = 0;
    }

    @Inject(method = "isKeyDown", at = @At("RETURN"), cancellable = true)
    private void nfr$mergeControllerDown(CallbackInfoReturnable<Boolean> callback) {
        if (nfr$controllerDown) callback.setReturnValue(true);
    }

    @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
    private void nfr$mergeControllerPress(CallbackInfoReturnable<Boolean> callback) {
        if (nfr$controllerPresses > 0) {
            nfr$controllerPresses--;
            callback.setReturnValue(true);
        }
    }
}
