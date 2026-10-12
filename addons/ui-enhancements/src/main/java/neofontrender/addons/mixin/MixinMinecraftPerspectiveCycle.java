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
    /**
     * 1.7.10 has no {@code processKeyBinds}: the F5 toggle is handled inline in {@code runTick}'s
     * keyboard loop, and its {@code isPressed()} call is the first one in that method (ordinal 0,
     * the smooth-camera toggle follows at ordinal 1). The handler stays vanilla for every other
     * binding, which {@link CameraPerspectiveController#consumePerspectiveKey} checks itself.
     */
    @Redirect(method = "runTick",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/client/settings/KeyBinding;isPressed()Z", ordinal = 0),
            require = 1)
    private boolean nfrUi$cycleCameraPerspective(KeyBinding binding) {
        return CameraPerspectiveController.consumePerspectiveKey(binding);
    }
}
