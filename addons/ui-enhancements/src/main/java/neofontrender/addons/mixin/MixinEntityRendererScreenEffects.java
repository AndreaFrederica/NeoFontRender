package neofontrender.addons.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import neofontrender.addons.effects.ScreenEffectsRenderer;
import neofontrender.addons.loading.WorldLoadingSnapshotManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererScreenEffects {
    @Unique
    private boolean nfrUi$worldRenderedThisFrame;

    @Inject(method = "updateCameraAndRender", at = @At("HEAD"))
    private void nfrUi$beginRenderedFrame(
            float partialTicks, long finishTimeNano, CallbackInfo ci) {
        nfrUi$worldRenderedThisFrame = false;
    }

    @Inject(
            method = "updateCameraAndRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/EntityRenderer;renderWorld(FJ)V",
                    shift = At.Shift.AFTER
            )
    )
    private void nfrUi$markFreshWorldFrame(
            float partialTicks, long finishTimeNano, CallbackInfo ci) {
        nfrUi$worldRenderedThisFrame = true;
    }

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
        // This boundary is after vanilla/global post processing but before UI blur, HUD, and
        // GuiScreen. Only consume a request when renderWorld actually ran in this invocation;
        // otherwise the main framebuffer may still contain a complete GUI from an older frame.
        if (nfrUi$worldRenderedThisFrame) {
            WorldLoadingSnapshotManager.INSTANCE.captureRequestedWorldFrame();
        }
        ScreenEffectsRenderer.INSTANCE.renderBeforeOverlay(partialTicks);
    }
}
