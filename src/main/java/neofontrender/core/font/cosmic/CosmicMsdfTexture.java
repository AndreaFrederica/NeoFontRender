package neofontrender.core.font.cosmic;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.IResourceManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.nio.ByteBuffer;

/** Three-channel RGB8 texture containing a normalized experimental MSDF. */
final class CosmicMsdfTexture extends AbstractTexture {
    CosmicMsdfTexture(byte[] pixels, int width, int height) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(pixels.length);
        buffer.put(pixels).flip();
        int texture = getGlTextureId();
        GlStateManager.bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        int previousAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        try {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, width, height, 0,
                    GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
        } finally {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, previousAlignment);
        }
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        // Upload happens in the constructor; this texture has no resource-backed source.
    }
}
