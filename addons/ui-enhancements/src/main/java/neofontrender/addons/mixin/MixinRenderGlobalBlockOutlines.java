package neofontrender.addons.mixin;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import neofontrender.addons.outlines.BlockOutlineResolver;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobalBlockOutlines {
    @Redirect(method = "drawSelectionBox",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/client/renderer/RenderGlobal;drawSelectionBoundingBox(Lnet/minecraft/util/math/AxisAlignedBB;FFFF)V"))
    private void nfrUi$drawConfiguredBlockOutline(AxisAlignedBB box, float red, float green,
                                                   float blue, float alpha, EntityPlayer player,
                                                   RayTraceResult target, int execute,
                                                   float partialTicks) {
        BlockOutlineResolver.ResolvedOutline outline = null;
        if (target != null && target.typeOfHit == RayTraceResult.Type.BLOCK && player != null) {
            BlockPos position = target.getBlockPos();
            IBlockState state = player.world.getBlockState(position);
            outline = BlockOutlineResolver.resolve(player, state, position);
        }
        if (outline == null) {
            RenderGlobal.drawSelectionBoundingBox(box, red, green, blue, alpha);
            return;
        }
        float previousWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        GL11.glLineWidth(outline.lineWidth);
        try {
            RenderGlobal.drawSelectionBoundingBox(box, outline.red, outline.green, outline.blue, outline.alpha);
        } finally {
            GL11.glLineWidth(previousWidth);
        }
    }
}
