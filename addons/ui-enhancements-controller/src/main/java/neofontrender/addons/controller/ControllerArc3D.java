package neofontrender.addons.controller;

import net.minecraft.client.renderer.GlStateManager;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightHudCanvas;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.function.Consumer;

/** Restores the ModularUI render state around UIE's Arc3D-backed canvas. */
final class ControllerArc3D {
    private ControllerArc3D() {}

    static void draw(Consumer<FlightHudCanvas> operation) {
        FlightHudCanvas canvas = FlightApi.getHudCanvas();
        if (canvas == null) return;
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        float lineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        try {
            GlStateManager.disableTexture2D();
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            operation.accept(canvas);
        } finally {
            GL11.glLineWidth(lineWidth);
            GL14.glBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
            if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
            if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
