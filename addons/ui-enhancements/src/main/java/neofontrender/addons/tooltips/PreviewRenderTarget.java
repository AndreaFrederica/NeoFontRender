package neofontrender.addons.tooltips;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;

/**
 * A reusable transparent preview surface; its depth clears never touch the container.
 *
 * <p>1.7.10 has no {@code GlStateManager} and no {@code BufferBuilder}: the caller's state is
 * snapshotted with {@code glPushAttrib} and the composite quad is emitted through
 * {@link Tessellator}.</p>
 */
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
        // 1.7.10 has no GlStateManager; the shared snapshot restores the attribute stack and the
        // texture bindings a foreign model renderer can move.
        try (ModernTooltipRenderer.CallerGlState ignored =
                     ModernTooltipRenderer.CallerGlState.capture()) {
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            int textureDepth = GL11.glGetInteger(GL11.GL_TEXTURE_STACK_DEPTH);
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glOrtho(0, width, height, 0, 1000, 3000);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glTranslatef(0, 0, -2000);
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
                GL11.glColorMask(true, true, true, true);
                GL11.glDepthMask(true);
                GL11.glClearColor(0, 0, 0, 0);
                GL11.glClearDepth(1);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthFunc(GL11.GL_LEQUAL);
                GL11.glDepthRange(0, 1);
                GL11.glDisable(GL11.GL_FOG);
                GL11.glEnable(GL11.GL_CULL_FACE);
                GL11.glCullFace(GL11.GL_BACK);
                GL11.glFrontFace(GL11.GL_CCW);
                GL11.glEnable(GL11.GL_NORMALIZE);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.01F);
                GL11.glEnable(GL11.GL_BLEND);
                OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glColor4f(1, 1, 1, 1);
                draw.run();
            } finally {
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldDraw);
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldRead);
                GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, oldRenderbuffer);
                GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                popToDepth(GL11.GL_MODELVIEW_STACK_DEPTH, modelDepth);
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                popToDepth(GL11.GL_PROJECTION_STACK_DEPTH, projectionDepth);
                OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
                GL11.glMatrixMode(GL11.GL_TEXTURE);
                popToDepth(GL11.GL_TEXTURE_STACK_DEPTH, textureDepth);
                GL11.glMatrixMode(matrixMode);
            }
        }
        return true;
    }

    private static void popToDepth(int parameter, int depth) {
        // A failing third-party model can leave additional matrix pushes behind.
        while (GL11.glGetInteger(parameter) > depth) GL11.glPopMatrix();
    }

    void composite(int x, int y, int width, int height, float opacity) {
        if (target == null) return;
        GL20.glUseProgram(0);
        prepareTextureUnits();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.001F);
        GL11.glEnable(GL11.GL_BLEND);
        // The transparent target contains premultiplied color. Fade RGB and alpha together.
        OpenGlHelper.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(opacity, opacity, opacity, opacity);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, target.framebufferTexture);
        Tessellator buffer = Tessellator.instance;
        buffer.startDrawingQuads();
        buffer.setColorRGBA_F(opacity, opacity, opacity, opacity);
        buffer.addVertexWithUV(x, y + height, 0, 0, 0);
        buffer.addVertexWithUV(x + width, y + height, 0, 1, 0);
        buffer.addVertexWithUV(x + width, y, 0, 1, 1);
        buffer.addVertexWithUV(x, y, 0, 0, 1);
        buffer.draw();
    }

    private static void prepareTextureUnits() {
        OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE2);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
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
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTexture == deletedTexture ? 0 : oldTexture);
    }
}
