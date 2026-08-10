package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBoat;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import neofontrender.addons.camera.CameraPickingService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces ItemBoat's single hard-coded player ray with the camera ray. */
@Mixin(ItemBoat.class)
public abstract class MixinItemBoatCameraRayTrace {
    @Redirect(method = "onItemRightClick",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/world/World;rayTraceBlocks(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Z)Lnet/minecraft/util/math/RayTraceResult;"),
            require = 1)
    private RayTraceResult nfrUi$cameraBoatRay(World world, Vec3d from, Vec3d to,
                                                boolean stopOnLiquid) {
        net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().player;
        if (!CameraPickingService.overridesInteractionBlockRay(player)) {
            return world.rayTraceBlocks(from, to, stopOnLiquid);
        }
        return CameraPickingService.traceInteractionBlocks(player, from.distanceTo(to),
                1.0F, stopOnLiquid, false, false);
    }
}
