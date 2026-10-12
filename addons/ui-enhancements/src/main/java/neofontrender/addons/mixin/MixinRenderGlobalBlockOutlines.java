package neofontrender.addons.mixin;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import neofontrender.addons.outlines.BlockOutlineRenderer;
import neofontrender.addons.outlines.BlockOutlineResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobalBlockOutlines {
    @Redirect(method = "drawSelectionBox",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/client/renderer/RenderGlobal;drawOutlinedBoundingBox(Lnet/minecraft/util/AxisAlignedBB;I)V"),
            require = 1)
    private void nfrUi$drawConfiguredBlockOutline(AxisAlignedBB box, int color,
                                                   EntityPlayer player, MovingObjectPosition target,
                                                   int execute, float partialTicks) {
        BlockOutlineResolver.ResolvedOutline outline = null;
        if (target != null && target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && player != null) {
            Block block = player.worldObj.getBlock(target.blockX, target.blockY, target.blockZ);
            int metadata = player.worldObj.getBlockMetadata(
                    target.blockX, target.blockY, target.blockZ);
            outline = BlockOutlineResolver.resolve(player, block, metadata);
        }
        if (outline == null) {
            RenderGlobal.drawOutlinedBoundingBox(box, color);
            return;
        }
        BlockOutlineRenderer.draw(box, outline);
    }
}
