package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBoat;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
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
                    "Lnet/minecraft/world/World;rayTraceBlocks(Lnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;Z)Lnet/minecraft/util/MovingObjectPosition;"),
            require = 1)
    private MovingObjectPosition nfrUi$cameraBoatRay(World world, Vec3 from, Vec3 to,
                                                     boolean stopOnLiquid) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (!CameraPickingService.overridesInteractionBlockRay(player)) {
            return world.rayTraceBlocks(from, to, stopOnLiquid);
        }
        return CameraPickingService.traceInteractionBlocks(player, from.distanceTo(to),
                1.0F, stopOnLiquid, false, false);
    }
}
