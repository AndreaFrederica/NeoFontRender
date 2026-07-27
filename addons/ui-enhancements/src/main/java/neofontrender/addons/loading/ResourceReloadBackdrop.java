package neofontrender.addons.loading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import neofontrender.addons.ui.NfrUiEnhancements;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/** Keeps the visible options screen alive while TextureManager itself is being reloaded. */
final class ResourceReloadBackdrop {
    private int texture;
    private int textureWidth;
    private int textureHeight;
    private boolean valid;

    void capture() {
        release();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0) return;
        int oldActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int oldReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int oldTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            texture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 33071);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 33071);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8,
                    mc.displayWidth, mc.displayHeight, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
            GL11.glReadBuffer(GL11.GL_FRONT);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0,
                    0, 0, mc.displayWidth, mc.displayHeight);
            textureWidth = mc.displayWidth;
            textureHeight = mc.displayHeight;
            valid = true;
        } catch (Throwable throwable) {
            NfrUiEnhancements.LOGGER.warn("Could not capture the resource-reload backdrop", throwable);
            release();
        } finally {
            GL11.glReadBuffer(oldReadBuffer);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTexture);
            GL13.glActiveTexture(oldActiveTexture);
        }
    }

    boolean draw(int width, int height) {
        if (!valid || texture == 0 || textureWidth <= 0 || textureHeight <= 0
                || width <= 0 || height <= 0) return false;
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(0.0D, height, 0.0D).tex(0.0D, 0.0D).endVertex();
        buffer.pos(width, height, 0.0D).tex(1.0D, 0.0D).endVertex();
        buffer.pos(width, 0.0D, 0.0D).tex(1.0D, 1.0D).endVertex();
        buffer.pos(0.0D, 0.0D, 0.0D).tex(0.0D, 1.0D).endVertex();
        Tessellator.getInstance().draw();
        return true;
    }

    void release() {
        if (texture != 0) GL11.glDeleteTextures(texture);
        texture = 0;
        textureWidth = 0;
        textureHeight = 0;
        valid = false;
    }
}
