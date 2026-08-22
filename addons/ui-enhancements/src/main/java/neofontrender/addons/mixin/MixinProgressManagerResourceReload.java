package neofontrender.addons.mixin;

import cpw.mods.fml.common.ProgressManager;
import neofontrender.addons.loading.ResourceReloadRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pseudo: same early-load protection as {@link MixinProgressBarResourceReload}. If
 * {@code ProgressManager} is already loaded (e.g. by Angelica) this hook is skipped instead of
 * aborting startup.
 */
@Pseudo
@Mixin(value = ProgressManager.class, remap = false)
public abstract class MixinProgressManagerResourceReload {
    @Inject(method = "pop", at = @At("HEAD"))
    private static void nfrUi$finishResourceBar(ProgressManager.ProgressBar bar, CallbackInfo ci) {
        ResourceReloadRenderer.INSTANCE.progressBarCompleted(bar.getTitle());
    }
}
