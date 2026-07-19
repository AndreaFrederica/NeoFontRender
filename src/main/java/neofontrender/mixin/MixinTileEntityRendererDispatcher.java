package neofontrender.mixin;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
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
        // Dispatcher.entityX/Y/Z is the player entity position, not necessarily the camera (third
        // person and detached camera modes offset it). Ray tests must originate at the same camera
        // position used by the frustum and by vanilla block rendering, otherwise visible signs can
        // be classified behind a wall or genuinely hidden signs can miss the wall entirely.
        Vec3d cameraOffset = ActiveRenderInfo.getCameraPosition();
        Vec3d camera = new Vec3d(entityX + cameraOffset.x, entityY + cameraOffset.y,
                entityZ + cameraOffset.z);
        if (tileEntity instanceof TileEntitySign && SignOcclusionCuller.shouldCull(
                (TileEntitySign) tileEntity, world, camera.x, camera.y, camera.z)) {
            ci.cancel();
        }
    }

    @Inject(method = "drawBatch", at = @At("HEAD"), remap = false)
    private void nfr$flushSignBatch(int pass, CallbackInfo ci) {
        SignBatchRenderer.flush(pass);
    }
}
