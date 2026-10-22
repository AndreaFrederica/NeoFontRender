package neofontrender.addons.tooltips;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;

/** A reusable transparent preview surface; its depth clears never touch the container. */
final class PreviewRenderTarget {
    private Framebuffer target;

    boolean render(int width, int height, float pixelScale, Runnable draw) {
        if (!OpenGlHelper.isFramebufferEnabled()) return false;
        int pixelsX = Math.min(2048, Math.max(32, Math.round(width * pixelScale)));
        int pixelsY = Math.min(2048, Math.max(32, Math.round(height * pixelScale)));
        int oldDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int oldRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int oldRenderbuffer = GL11.glGetInteger(GL30.GL_RENDERBUFFER_BINDING);
        int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int modelDepth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        int projectionDepth = GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH);
        IntBuffer viewport = BufferUtils.createIntBuffer(4);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        try (ModernTooltipRenderer.CallerGlState ignored = ModernTooltipRenderer.CallerGlState.capture()) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            int textureDepth = GL11.glGetInteger(GL11.GL_TEXTURE_STACK_DEPTH);
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0, width, height, 0, 1000, 3000);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.translate(0, 0, -2000);
            try {
                GL20.glUseProgram(0);
                prepareTextureUnits();
                if (target == null || target.framebufferWidth != pixelsX || target.framebufferHeight != pixelsY) {
                    release();
                    target = new Framebuffer(pixelsX, pixelsY, true);
                    target.setFramebufferColor(0, 0, 0, 0);
                    target.setFramebufferFilter(GL11.GL_LINEAR);
                }
                target.bindFramebuffer(true);
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GlStateManager.colorMask(true, true, true, true);
                GlStateManager.depthMask(true);
                GL11.glClearColor(0, 0, 0, 0);
                GL11.glClearDepth(1);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                GlStateManager.enableDepth();
                GlStateManager.depthFunc(GL11.GL_LEQUAL);
                GL11.glDepthRange(0, 1);
                GlStateManager.disableFog();
                GlStateManager.enableCull();
                GlStateManager.cullFace(GlStateManager.CullFace.BACK);
                GL11.glFrontFace(GL11.GL_CCW);
                GL11.glEnable(GL11.GL_NORMALIZE);
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01F);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GlStateManager.color(1, 1, 1, 1);
                draw.run();
            } finally {
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldDraw);
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldRead);
                GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, oldRenderbuffer);
                GlStateManager.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                popToDepth(GL11.GL_MODELVIEW_STACK_DEPTH, modelDepth);
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                popToDepth(GL11.GL_PROJECTION_STACK_DEPTH, projectionDepth);
                GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.matrixMode(GL11.GL_TEXTURE);
                popToDepth(GL11.GL_TEXTURE_STACK_DEPTH, textureDepth);
                GlStateManager.matrixMode(matrixMode);
            }
        }
        return true;
    }

    private static void popToDepth(int parameter, int depth) {
        // A failing third-party model can leave additional matrix pushes behind.
        while (GL11.glGetInteger(parameter) > depth) GlStateManager.popMatrix();
    }

    void composite(int x, int y, int width, int height, float opacity) {
        if (target == null) return;
        GL20.glUseProgram(0);
        prepareTextureUnits();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.001F);
        GlStateManager.enableBlend();
        // The transparent target contains premultiplied color. Fade RGB and alpha together.
        GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(opacity, opacity, opacity, opacity);
        GlStateManager.bindTexture(target.framebufferTexture);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + height, 0).tex(0, 0).endVertex();
        buffer.pos(x + width, y + height, 0).tex(1, 0).endVertex();
        buffer.pos(x + width, y, 0).tex(1, 1).endVertex();
        buffer.pos(x, y, 0).tex(0, 1).endVertex();
        Tessellator.getInstance().draw();
    }

    private static void prepareTextureUnits() {
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE2);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
    }

    void release() {
        if (target == null) return;
        int oldDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int oldRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int oldTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int deletedFbo = target.framebufferObject;
        int deletedTexture = target.framebufferTexture;
        // GuiOpenEvent can run while Minecraft's main target is still bound.
        // Framebuffer.deleteFramebuffer() unconditionally unbinds it and the active texture.
        target.deleteFramebuffer();
        target = null;
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldDraw == deletedFbo ? 0 : oldDraw);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldRead == deletedFbo ? 0 : oldRead);
        GlStateManager.bindTexture(oldTexture == deletedTexture ? 0 : oldTexture);
    }
}
