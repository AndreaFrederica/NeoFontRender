package neofontrender.addons.mixin;

import cpw.mods.fml.common.ProgressManager;
import neofontrender.addons.loading.ResourceReloadRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ProgressManager.class, remap = false)
public abstract class MixinProgressManagerResourceReload {
    @Inject(method = "pop", at = @At("HEAD"))
    private static void nfrUi$finishResourceBar(ProgressManager.ProgressBar bar, CallbackInfo ci) {
        ResourceReloadRenderer.INSTANCE.progressBarCompleted(bar.getTitle());
    }
}
