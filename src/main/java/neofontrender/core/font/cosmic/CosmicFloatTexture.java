package neofontrender.core.font.cosmic;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.IResourceManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

/** Uploads one straight-alpha Cosmic raster as an RGBA16F premultiplied texture. */
final class CosmicFloatTexture extends AbstractTexture {
    private final int width;
    private final int height;

    CosmicFloatTexture(int[] straightPixels, int width, int height) {
        this.width = width;
        this.height = height;
        FloatBuffer pixels = BufferUtils.createFloatBuffer(width * height * 4);
        for (int pixel : straightPixels) {
            float alpha = ((pixel >>> 24) & 0xFF) / 255.0F;
            pixels.put(((pixel >>> 16) & 0xFF) / 255.0F * alpha);
            pixels.put(((pixel >>> 8) & 0xFF) / 255.0F * alpha);
            pixels.put((pixel & 0xFF) / 255.0F * alpha);
            pixels.put(alpha);
        }
        pixels.flip();

        int texture = getGlTextureId();
        GlStateManager.bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, width, height, 0,
                GL11.GL_RGBA, GL11.GL_FLOAT, pixels);
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        // The pixels are uploaded at construction time and are not resource-backed.
    }
}
