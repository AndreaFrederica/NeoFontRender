package neofontrender.addons.mixin;

import cpw.mods.fml.common.ProgressManager;
import neofontrender.addons.loading.ResourceReloadRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pseudo: some environments (e.g. Angelica) load {@code ProgressManager$ProgressBar} before the
 * mixin system prepares this config. With {@code @Pseudo} and a non-required config the step
 * hook is skipped there instead of crashing the game with MixinTargetAlreadyLoadedException.
 */
@Pseudo
@Mixin(value = ProgressManager.ProgressBar.class, remap = false)
public abstract class MixinProgressBarResourceReload {
    @Inject(method = "step(Ljava/lang/String;)V", at = @At("HEAD"))
    private void nfrUi$beforeResourceStep(String message, CallbackInfo ci) {
        ProgressManager.ProgressBar self = (ProgressManager.ProgressBar) (Object) this;
        ResourceReloadRenderer.INSTANCE.beforeProgressStep(
                self.getTitle(), self.getStep(), self.getSteps(), message);
    }
}
