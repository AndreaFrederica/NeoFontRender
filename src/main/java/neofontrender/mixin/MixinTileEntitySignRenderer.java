package neofontrender.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.AxisAlignedBB;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.support.FontRenderTuning;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional sign-only optimizations. The mixin config plugin prevents this class from being
 * applied at launch when all sign options are disabled, preserving the vanilla call graph.
 */
@Mixin(TileEntitySignRenderer.class)
public abstract class MixinTileEntitySignRenderer {
    @Unique private static int nfr$wallSignLodList;
    @Unique private static int nfr$standingSignLodList;
    @Unique private double nfr$distanceSq;

    @Inject(method = "renderTileEntityAt", at = @At("HEAD"), cancellable = true)
    private void nfr$cullSignModel(TileEntitySign sign, double x, double y, double z,
                                   float partialTicks, CallbackInfo ci) {
        nfr$distanceSq = x * x + y * y + z * z;
        if (!NeofontrenderConfig.signTextFrustumCulling()) {
            return;
        }
        // Dispatcher coordinates are relative to the camera-facing render origin. The sign model
        // is translated to the block center, and a rotated standing sign reaches about 0.71 blocks
        // from that center. The extra margin also covers board depth and wall-sign offsets.
        Frustrum frustrum = new Frustrum();
        frustrum.setPosition(0.0D, 0.0D, 0.0D);
        double centerX = x + 0.5D;
        double centerZ = z + 0.5D;
        AxisAlignedBB bounds = AxisAlignedBB.getBoundingBox(
                centerX - 0.85D, y - 0.15D, centerZ - 0.85D,
                centerX + 0.85D, y + 1.20D, centerZ + 0.85D);
        if (!frustrum.isBoundingBoxInFrustum(bounds)) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "renderTileEntityAt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelSign;renderSign()V")
    )
    private void nfr$renderSignModelLod(ModelSign model) {
        float lodDistance = NeofontrenderConfig.signModelLodDistance();
        if (!NeofontrenderConfig.signModelLod()
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
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        // ModelSign's board front/back occupy these regions in the currently bound 64x32
        // resource-pack texture. Keeping its UVs avoids manufacturing or caching a second texture.
        nfr$quad(tessellator, -0.75F, -0.875F, -0.0625F, 0.75F, -0.125F,
                2.0F / 64.0F, 2.0F / 32.0F, 26.0F / 64.0F, 14.0F / 32.0F, -1.0F);
        nfr$quad(tessellator, 0.75F, -0.875F, 0.0625F, -0.75F, -0.125F,
                28.0F / 64.0F, 2.0F / 32.0F, 52.0F / 64.0F, 14.0F / 32.0F, 1.0F);
        if (standing) {
            nfr$quad(tessellator, -0.0625F, -0.125F, -0.0625F, 0.0625F, 0.75F,
                    2.0F / 64.0F, 16.0F / 32.0F, 4.0F / 64.0F, 30.0F / 32.0F, -1.0F);
        }
        tessellator.draw();
        GL11.glEndList();
        return list;
    }

    @Unique
    private static void nfr$quad(Tessellator tessellator, float left, float top, float z,
                                 float right, float bottom, float u0, float v0, float u1, float v1,
                                 float normalZ) {
        tessellator.setNormal(0.0F, 0.0F, normalZ);
        tessellator.addVertexWithUV(left, top, z, u0, v0);
        tessellator.addVertexWithUV(left, bottom, z, u0, v1);
        tessellator.addVertexWithUV(right, bottom, z, u1, v1);
        tessellator.addVertexWithUV(right, top, z, u1, v0);
    }

    @Redirect(
            method = "renderTileEntityAt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"
            )
    )
    private int nfr$drawSignLine(FontRenderer renderer, String text, int x, int y, int color) {
        if (!NeofontrenderConfig.signTextLodCulling()) {
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
