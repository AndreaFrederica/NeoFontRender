package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blocks player interaction entry points while the drone input context is active. */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftDroneInputGate {
    /**
     * 1.7.10 has no {@code processKeyBinds}: {@code runTick} handles the keys inline, and the
     * press edges of the tick are registered by the "keyboard" event loop before the hotbar
     * loop. Draining the blocked keys at the hotbar loop's {@code isPressed()} call (ordinal 2
     * of {@code runTick}, after the perspective and smooth-camera toggles) discards them before
     * the hotbar, inventory and drop loops can consume them. The drain is idempotent, so the
     * loop's nine iterations are harmless.
     */
    @Inject(method = "runTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/settings/KeyBinding;isPressed()Z", ordinal = 2),
            require = 1)
    private void nfrUi$drainBlockedDroneCommands(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (InputApi.isBlocked(InputAction.PLAYER_DROP)) {
            while (minecraft.gameSettings.keyBindDrop.isPressed()) { }
        }
        if (InputApi.isBlocked(InputAction.PLAYER_INVENTORY)) {
            while (minecraft.gameSettings.keyBindInventory.isPressed()) { }
        }
        // 1.7.10 has no off-hand key bind, so PLAYER_SWAP_HANDS needs no draining here.
        if (InputApi.isBlocked(InputAction.PLAYER_HOTBAR)) {
            for (KeyBinding key : minecraft.gameSettings.keyBindsHotbar) {
                while (key.isPressed()) { }
            }
        }
    }

    /** 1.7.10 keeps the 1.12 clickMouse entry point in this private attack handler. */
    @Inject(method = "func_147116_af", at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$blockDroneAttack(CallbackInfo ci) {
        if (InputApi.isBlocked(InputAction.PLAYER_ATTACK)) ci.cancel();
    }

    /** 1.7.10 keeps the 1.12 rightClickMouse entry point in this private use handler. */
    @Inject(method = "func_147121_ag", at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$blockDroneUse(CallbackInfo ci) {
        if (InputApi.isBlocked(InputAction.PLAYER_USE)) ci.cancel();
    }

    /** 1.7.10 keeps the 1.12 middleClickMouse entry point in this private pick-block handler. */
    @Inject(method = "func_147112_ai", at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$blockDronePickBlock(CallbackInfo ci) {
        if (InputApi.isBlocked(InputAction.PLAYER_PICK_BLOCK)) ci.cancel();
    }

    /** 1.7.10 equivalent of sendClickBlockToController, boolean left-click parameter included. */
    @Inject(method = "func_147115_a(Z)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$stopDroneContinuousMining(boolean leftClick, CallbackInfo ci) {
        if (!InputApi.isBlocked(InputAction.PLAYER_ATTACK)) return;
        Minecraft minecraft = (Minecraft) (Object) this;
        if (minecraft.playerController != null) minecraft.playerController.resetBlockRemoving();
        ci.cancel();
    }
}
