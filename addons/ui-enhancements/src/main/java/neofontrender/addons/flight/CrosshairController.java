package neofontrender.addons.flight;

import icyllis.arc3d.core.Color;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Last-priority crosshair renderer; vanilla suppression is performed by a narrow mixin. */
public final class CrosshairController {
    static final CrosshairController INSTANCE = new CrosshairController();

    private CrosshairController() {}

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void crosshair(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        if (!CrosshairConfig.customEnabled
                || FlightRollController.suppressVanillaCrosshair()) return;
        draw(event.getResolution().getScaledWidth() * 0.5F,
                event.getResolution().getScaledHeight() * 0.5F);
    }

    /** Used by the Forge-GUI mixin; deliberately does not cancel the CROSSHAIRS event. */
    public static boolean suppressVanillaCrosshair() {
        return CrosshairConfig.customEnabled || FlightRollController.suppressVanillaCrosshair();
    }

    private static void draw(float cx, float cy) {
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        float oldWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        int blendSourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int blendDestinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int blendSourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int blendDestinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        try {
            float scale = CrosshairConfig.scalePercent / 100.0F;
            float gap = CrosshairConfig.gap * scale;
            float arm = CrosshairConfig.armLength * scale;
            int color = CrosshairConfig.color;
            switch (CrosshairConfig.style) {
                case "dot":
                    quad(cx - Math.max(1.0F, scale), cy - Math.max(1.0F, scale),
                            cx + Math.max(1.0F, scale), cy + Math.max(1.0F, scale), color);
                    break;
                case "circle":
                    circle(cx, cy, gap + arm, color, CrosshairConfig.thickness);
                    break;
                case "chevron":
                    line(cx - gap - arm, cy - gap - arm * 0.5F, cx, cy + gap,
                            color, CrosshairConfig.thickness);
                    line(cx, cy + gap, cx + gap + arm, cy - gap - arm * 0.5F,
                            color, CrosshairConfig.thickness);
                    break;
                default:
                    line(cx - gap - arm, cy, cx - gap, cy, color, CrosshairConfig.thickness);
                    line(cx + gap, cy, cx + gap + arm, cy, color, CrosshairConfig.thickness);
                    line(cx, cy - gap - arm, cx, cy - gap, color, CrosshairConfig.thickness);
                    line(cx, cy + gap, cx, cy + gap + arm, color, CrosshairConfig.thickness);
                    break;
            }
        } finally {
            GL11.glLineWidth(oldWidth);
            GL14.glBlendFuncSeparate(blendSourceRgb, blendDestinationRgb,
                    blendSourceAlpha, blendDestinationAlpha);
            if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
            if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        }
    }

    private static void circle(float cx, float cy, float radius, int color, float width) {
        GL11.glLineWidth(Math.max(1.0F, width));
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 32; i++) {
            double angle = Math.PI * 2.0D * i / 32.0D;
            vertex(buffer, cx + (float) Math.cos(angle) * radius,
                    cy + (float) Math.sin(angle) * radius, color);
        }
        tessellator.draw();
    }

    private static void line(float x1, float y1, float x2, float y2, int color, float width) {
        GL11.glLineWidth(Math.max(1.0F, width));
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, x1, y1, color); vertex(buffer, x2, y2, color);
        tessellator.draw();
    }

    private static void quad(float left, float top, float right, float bottom, int color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, left, top, color); vertex(buffer, left, bottom, color);
        vertex(buffer, right, bottom, color); vertex(buffer, right, top, color);
        tessellator.draw();
    }

    private static void vertex(BufferBuilder buffer, float x, float y, int color) {
        buffer.pos(x, y, 0.0D).color(Color.red(color), Color.green(color),
                Color.blue(color), Color.alpha(color)).endVertex();
    }
}
