package neofontrender.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.AxisAlignedBB;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.support.FontRenderTuning;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.opengl.GL11;

/**
 * Optional sign-only optimizations. The mixin config plugin prevents this class from being
 * applied at launch when both sign options are disabled, preserving the vanilla call graph.
 */
@Mixin(TileEntitySignRenderer.class)
public abstract class MixinTileEntitySignRenderer {
    @Unique private static int nfr$wallSignLodList;
    @Unique private static int nfr$standingSignLodList;
    @Unique private double nfr$distanceSq;
    @Unique private int nfr$destroyStage;

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntitySign;DDDFIF)V", at = @At("HEAD"), cancellable = true)
    private void nfr$cullSignModel(TileEntitySign sign, double x, double y, double z,
                                   float partialTicks, int destroyStage, float alpha,
                                   CallbackInfo ci) {
        nfr$distanceSq = x * x + y * y + z * z;
        nfr$destroyStage = destroyStage;
        if (!NeofontrenderConfig.signTextFrustumCulling()) {
            return;
        }
        // Dispatcher coordinates are the camera-relative block origin, while vanilla translates
        // the model to the block center (x/z + 0.5) before rotating it. The old box was centered on
        // the origin and therefore sat half a block too far toward negative X/Z, culling a visible
        // edge for signs near the right/top frustum planes. A standing sign's rotated 1-block-wide
        // board reaches about 0.71 blocks from center; 0.85 also covers its depth and wall offset.
        Frustum frustum = new Frustum();
        frustum.setPosition(0.0D, 0.0D, 0.0D);
        double centerX = x + 0.5D;
        double centerZ = z + 0.5D;
        AxisAlignedBB bounds = new AxisAlignedBB(centerX - 0.85D, y - 0.15D, centerZ - 0.85D,
                centerX + 0.85D, y + 1.20D, centerZ + 0.85D);
        if (!frustum.isBoundingBoxInFrustum(bounds)) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "render(Lnet/minecraft/tileentity/TileEntitySign;DDDFIF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelSign;renderSign()V")
    )
    private void nfr$renderSignModelLod(ModelSign model) {
        float lodDistance = NeofontrenderConfig.signModelLodDistance();
        if (!NeofontrenderConfig.signModelLod() || nfr$destroyStage >= 0
                || nfr$distanceSq < lodDistance * lodDistance) {
            model.renderSign();
            return;
        }
        boolean standing = model.signStick.showModel;
        int list = standing ? nfr$standingSignLodList : nfr$wallSignLodList;
        if (list == 0) {
            list = nfr$compileSignLod(standing);
            if (standing) {
                nfr$standingSignLodList = list;
            } else {
                nfr$wallSignLodList = list;
            }
        }
        GL11.glCallList(list);
    }

    @Unique
    private static int nfr$compileSignLod(boolean standing) {
        int list = GLAllocation.generateDisplayLists(1);
        GL11.glNewList(list, GL11.GL_COMPILE);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);
        // ModelSign's board front/back occupy these regions in the currently bound 64x32
        // resource-pack texture. Keeping its UVs avoids manufacturing or caching a second texture.
        nfr$quad(buffer, -0.75F, -0.875F, -0.0625F, 0.75F, -0.125F,
                2.0F / 64.0F, 2.0F / 32.0F, 26.0F / 64.0F, 14.0F / 32.0F, -1.0F);
        nfr$quad(buffer, 0.75F, -0.875F, 0.0625F, -0.75F, -0.125F,
                28.0F / 64.0F, 2.0F / 32.0F, 52.0F / 64.0F, 14.0F / 32.0F, 1.0F);
        if (standing) {
            nfr$quad(buffer, -0.0625F, -0.125F, -0.0625F, 0.0625F, 0.75F,
                    2.0F / 64.0F, 16.0F / 32.0F, 4.0F / 64.0F, 30.0F / 32.0F, -1.0F);
        }
        tessellator.draw();
        GL11.glEndList();
        return list;
    }

    @Unique
    private static void nfr$quad(BufferBuilder buffer, float left, float top, float z,
                                 float right, float bottom, float u0, float v0, float u1, float v1,
                                 float normalZ) {
        buffer.pos(left, top, z).tex(u0, v0).normal(0.0F, 0.0F, normalZ).endVertex();
        buffer.pos(left, bottom, z).tex(u0, v1).normal(0.0F, 0.0F, normalZ).endVertex();
        buffer.pos(right, bottom, z).tex(u1, v1).normal(0.0F, 0.0F, normalZ).endVertex();
        buffer.pos(right, top, z).tex(u1, v0).normal(0.0F, 0.0F, normalZ).endVertex();
    }

    @Redirect(
            method = "render(Lnet/minecraft/tileentity/TileEntitySign;DDDFIF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"
            )
    )
    private int nfr$drawSignLine(FontRenderer renderer, String text, int x, int y, int color) {
        boolean lod = NeofontrenderConfig.signTextLodCulling();
        if (!lod) {
            return renderer.drawString(text, x, y, color);
        }

        FontRenderTuning.updateFromCurrentGlState(false);
        int width = renderer.getStringWidth(text);
        if (!FontRenderTuning.isCurrentTextQuadVisible(
                x, y, width, renderer.FONT_HEIGHT, NeofontrenderConfig.signTextMinPixelHeight())) {
            return x + width;
        }
        return renderer.drawString(text, x, y, color);
    }

}
