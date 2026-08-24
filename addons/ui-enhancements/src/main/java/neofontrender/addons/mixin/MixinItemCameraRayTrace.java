package neofontrender.addons.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import neofontrender.addons.camera.CameraPickingService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers ranged item use paths that call Item#rayTrace directly. */
@Mixin(Item.class)
public abstract class MixinItemCameraRayTrace {
    @Inject(method = "rayTrace(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Z)Lnet/minecraft/util/math/RayTraceResult;",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$cameraItemRay(World world, EntityPlayer player, boolean useLiquids,
                                     CallbackInfoReturnable<RayTraceResult> cir) {
        if (player == null) return;
        double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
        if (!CameraPickingService.overridesInteractionBlockRay(player)) return;
        cir.setReturnValue(CameraPickingService.traceInteractionBlocks(player, reach,
                1.0F, useLiquids, !useLiquids, false));
    }
}
