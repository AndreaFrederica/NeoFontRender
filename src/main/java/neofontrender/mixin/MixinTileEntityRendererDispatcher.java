package neofontrender.mixin;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;
import neofontrender.client.render.sign.SignOcclusionCuller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adapts the 1.7.10 tile-entity dispatch boundary to sign occlusion checks. */
@Mixin(TileEntityRendererDispatcher.class)
public abstract class MixinTileEntityRendererDispatcher {
    @Shadow public static double staticPlayerX;
    @Shadow public static double staticPlayerY;
    @Shadow public static double staticPlayerZ;

    @Inject(method = "renderTileEntity", at = @At("HEAD"), cancellable = true)
    private void nfr$cullOccludedSign(TileEntity tileEntity, float partialTicks, CallbackInfo ci) {
        World renderWorld = tileEntity.getWorldObj();
        SignOcclusionCuller.beginFrame(renderWorld);
        if (!(tileEntity instanceof TileEntitySign)) {
            return;
        }
        TileEntitySign sign = (TileEntitySign) tileEntity;
        if (SignOcclusionCuller.shouldCull(
                sign, renderWorld, staticPlayerX, staticPlayerY, staticPlayerZ)) {
            ci.cancel();
        }
    }
}
