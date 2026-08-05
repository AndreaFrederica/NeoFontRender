package neofontrender.addons.mixin;

import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.IRenderChunkFactory;
import neofontrender.addons.loading.WorldLoadingRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the window responsive while the client synchronously creates its render-chunk grid. */
@Mixin(ViewFrustum.class)
public abstract class MixinViewFrustumLoadingProgress {
    @Shadow protected int countChunksX;
    @Shadow protected int countChunksY;
    @Shadow protected int countChunksZ;

    @Inject(method = "createRenderChunks", at = @At("HEAD"), require = 0)
    private void nfrUi$beginRenderChunkCreation(IRenderChunkFactory factory, CallbackInfo ci) {
        WorldLoadingRenderer.INSTANCE.beginClientRenderChunkBatch(
                countChunksX * countChunksY * countChunksZ);
    }

    @Inject(method = "createRenderChunks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;setPosition(III)V",
            shift = At.Shift.AFTER), require = 0)
    private void nfrUi$recordCreatedRenderChunk(IRenderChunkFactory factory, CallbackInfo ci) {
        WorldLoadingRenderer.INSTANCE.recordClientRenderChunk();
    }

    @Inject(method = "createRenderChunks", at = @At("RETURN"), require = 0)
    private void nfrUi$finishRenderChunkCreation(IRenderChunkFactory factory, CallbackInfo ci) {
        WorldLoadingRenderer.INSTANCE.finishClientRenderChunkBatch();
    }
}
