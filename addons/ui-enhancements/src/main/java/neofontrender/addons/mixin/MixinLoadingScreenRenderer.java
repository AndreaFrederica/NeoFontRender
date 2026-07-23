package neofontrender.addons.mixin;

import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import neofontrender.addons.loading.WorldLoadingRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingScreenRenderer.class)
public abstract class MixinLoadingScreenRenderer {
    /**
     * LoadingScreenRenderer normally rejects frames arriving less than 100 ms apart. Lift only the
     * integrated-world presentation to about 60 FPS; all other vanilla loading paths keep 100 ms.
     */
    @ModifyConstant(method = "setLoadingProgress", constant = @Constant(longValue = 100L))
    private long nfrUi$smoothIntegratedLoadingFrames(long original) {
        return WorldLoadingRenderer.INSTANCE.isIntegratedLaunchActive() ? 16L : original;
    }

    /**
     * Draw after vanilla has prepared its loading framebuffer but before it is presented. This
     * preserves Minecraft's framebuffer/updateDisplay plumbing and only replaces the visual layer.
     */
    @Inject(method = "setLoadingProgress", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/shader/Framebuffer;unbindFramebuffer()V"))
    private void nfrUi$drawIntegratedServerProgress(int vanillaProgress, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc);
        WorldLoadingRenderer.INSTANCE.renderIntegratedServerLoading(
                resolution.getScaledWidth(), resolution.getScaledHeight(), vanillaProgress);
    }
}
