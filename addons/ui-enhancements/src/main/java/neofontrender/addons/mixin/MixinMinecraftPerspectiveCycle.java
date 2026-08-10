package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import neofontrender.addons.camera.CameraPerspectiveController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces only vanilla F5's perspective keypress with the UIE mode cycle. */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftPerspectiveCycle {
    @Redirect(method = "processKeyBinds",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/client/settings/KeyBinding;isPressed()Z", ordinal = 0),
            require = 1)
    private boolean nfrUi$cycleCameraPerspective(KeyBinding binding) {
        return CameraPerspectiveController.consumePerspectiveKey(binding);
    }
}
