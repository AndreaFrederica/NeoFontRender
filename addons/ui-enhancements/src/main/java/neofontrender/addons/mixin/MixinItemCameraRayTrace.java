package neofontrender.addons.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import neofontrender.addons.camera.CameraPickingService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Covers item use paths that build their own player ray.
 *
 * <p>1.7.10 has no {@code Item#rayTrace}; {@code getMovingObjectPositionFromPlayer} is the shared
 * helper every such item (bucket, glass bottle, ender eye, lily pad, monster placer) calls, and it
 * reaches as far as vanilla 1.7.10 does for the same player.
 */
@Mixin(Item.class)
public abstract class MixinItemCameraRayTrace {
    @Inject(method = "getMovingObjectPositionFromPlayer(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Z)Lnet/minecraft/util/MovingObjectPosition;",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$cameraItemRay(World world, EntityPlayer player, boolean useLiquids,
                                     CallbackInfoReturnable<MovingObjectPosition> cir) {
        if (player == null) return;
        if (!CameraPickingService.overridesInteractionBlockRay(player)) return;
        double reach = 5.0D;
        if (player instanceof EntityPlayerMP) {
            reach = ((EntityPlayerMP) player).theItemInWorldManager.getBlockReachDistance();
        }
        cir.setReturnValue(CameraPickingService.traceInteractionBlocks(player, reach,
                1.0F, useLiquids, !useLiquids, false));
    }
}
