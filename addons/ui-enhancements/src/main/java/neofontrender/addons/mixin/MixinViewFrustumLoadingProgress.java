package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import neofontrender.addons.loading.WorldLoadingRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the window responsive while the client synchronously creates its render-chunk grid.
 *
 * <p>1.7.10 has no {@code ViewFrustum}: the grid is built by {@code RenderGlobal.loadRenderers()},
 * which constructs one {@code WorldRenderer} per chunk section of a
 * {@code (renderDistanceChunks * 2 + 1)^2 * 16} grid. The class name is kept from the newer
 * platform so the registered mixin configuration stays valid.</p>
 */
@Mixin(RenderGlobal.class)
public abstract class MixinViewFrustumLoadingProgress {
    @Inject(method = "loadRenderers", at = @At("HEAD"))
    private void nfrUi$beginRenderChunkCreation(CallbackInfo ci) {
        int horizontal = Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 2 + 1;
        WorldLoadingRenderer.INSTANCE.beginClientRenderChunkBatch(horizontal * horizontal * 16);
    }

    @Inject(method = "loadRenderers", at = @At(value = "NEW",
            target = "net/minecraft/client/renderer/WorldRenderer", shift = At.Shift.AFTER),
            require = 0)
    private void nfrUi$recordCreatedRenderChunk(CallbackInfo ci) {
        WorldLoadingRenderer.INSTANCE.recordClientRenderChunk();
    }

    @Inject(method = "loadRenderers", at = @At("RETURN"))
    private void nfrUi$finishRenderChunkCreation(CallbackInfo ci) {
        WorldLoadingRenderer.INSTANCE.finishClientRenderChunkBatch();
    }
}
