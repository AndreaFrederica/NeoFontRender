package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blocks player interaction entry points while the drone input context is active. */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftDroneInputGate {
    @Inject(method = "processKeyBinds", at = @At("HEAD"), require = 1)
    private void nfrUi$drainBlockedDroneCommands(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (InputApi.isBlocked(InputAction.PLAYER_DROP)) {
            while (minecraft.gameSettings.keyBindDrop.isPressed()) { }
        }
        if (InputApi.isBlocked(InputAction.PLAYER_INVENTORY)) {
            while (minecraft.gameSettings.keyBindInventory.isPressed()) { }
        }
        if (InputApi.isBlocked(InputAction.PLAYER_SWAP_HANDS)) {
            while (minecraft.gameSettings.keyBindSwapHands.isPressed()) { }
        }
        if (InputApi.isBlocked(InputAction.PLAYER_HOTBAR)) {
            for (net.minecraft.client.settings.KeyBinding key : minecraft.gameSettings.keyBindsHotbar) {
                while (key.isPressed()) { }
            }
        }
    }

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$blockDroneAttack(CallbackInfo ci) {
        if (InputApi.isBlocked(InputAction.PLAYER_ATTACK)) ci.cancel();
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$blockDroneUse(CallbackInfo ci) {
        if (InputApi.isBlocked(InputAction.PLAYER_USE)) ci.cancel();
    }

    @Inject(method = "middleClickMouse", at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$blockDronePickBlock(CallbackInfo ci) {
        if (InputApi.isBlocked(InputAction.PLAYER_PICK_BLOCK)) ci.cancel();
    }

    @Inject(method = "sendClickBlockToController", at = @At("HEAD"), cancellable = true,
            require = 1)
    private void nfrUi$stopDroneContinuousMining(boolean leftClick, CallbackInfo ci) {
        if (!InputApi.isBlocked(InputAction.PLAYER_ATTACK)) return;
        Minecraft minecraft = (Minecraft) (Object) this;
        if (minecraft.playerController != null) minecraft.playerController.resetBlockRemoving();
        ci.cancel();
    }

    @Redirect(method = "runTickMouse", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/entity/player/InventoryPlayer;changeCurrentItem(I)V"), require = 1)
    private void nfrUi$blockDroneMouseWheelHotbar(InventoryPlayer inventory, int direction) {
        if (!InputApi.isBlocked(InputAction.PLAYER_HOTBAR)) inventory.changeCurrentItem(direction);
    }
}
