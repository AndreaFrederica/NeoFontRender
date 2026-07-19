package neofontrender.mixin;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;
import neofontrender.client.render.sign.SignBatchRenderer;
import neofontrender.client.render.sign.SignOcclusionCuller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge's TESR batch boundary gives sign collection a frame-local begin/flush pair. */
@Mixin(TileEntityRendererDispatcher.class)
public abstract class MixinTileEntityRendererDispatcher {
    @Shadow public World world;
    @Shadow public double entityX;
    @Shadow public double entityY;
    @Shadow public double entityZ;

    @Inject(method = "preDrawBatch", at = @At("TAIL"), remap = false)
    private void nfr$beginSignBatch(CallbackInfo ci) {
        SignOcclusionCuller.beginFrame(world);
        SignBatchRenderer.begin();
    }

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At("HEAD"), cancellable = true)
    private void nfr$cullOccludedSign(TileEntity tileEntity, float partialTicks, int destroyStage,
                                      CallbackInfo ci) {
        if (tileEntity instanceof TileEntitySign && SignOcclusionCuller.shouldCull(
                (TileEntitySign) tileEntity, world, entityX, entityY, entityZ)) {
            ci.cancel();
        }
    }

    @Inject(method = "drawBatch", at = @At("HEAD"), remap = false)
    private void nfr$flushSignBatch(int pass, CallbackInfo ci) {
        SignBatchRenderer.flush(pass);
    }
}
