package neofontrender.addons.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import neofontrender.addons.effects.ScreenEffectsRenderer;
import neofontrender.addons.loading.WorldLoadingSnapshotManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererScreenEffects {
    @Inject(
            method = "updateCameraAndRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/EntityRenderer;setupOverlayRendering()V",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            )
    )
    private void nfrUi$renderScreenEffectsBeforeOverlay(
            float partialTicks, long finishTimeNano, CallbackInfo ci) {
        // This first overlay boundary is reached only from the world-rendering branch, after the
        // world frame and global post-processing are complete. Unlike an injection on renderWorld,
        // it survives Somnia's static render bridge and Cleanroom's render delegate.
        WorldLoadingSnapshotManager.INSTANCE.captureRequestedWorldFrame();
        ScreenEffectsRenderer.INSTANCE.renderBeforeOverlay(partialTicks);
    }
}
