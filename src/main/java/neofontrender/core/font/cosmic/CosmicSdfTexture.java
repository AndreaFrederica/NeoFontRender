package neofontrender.core.font.cosmic;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.IResourceManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.nio.ByteBuffer;

/** OpenGL single-channel texture containing a normalized signed-distance field. */
final class CosmicSdfTexture extends AbstractTexture {
    CosmicSdfTexture(byte[] pixels, int width, int height) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(pixels.length);
        buffer.put(pixels).flip();
        int texture = getGlTextureId();
        GlStateManager.bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        // R8 rows are one byte per texel. Minecraft and other mods commonly leave the unpack
        // alignment at 4, which shears every row whose width is not divisible by four and turns
        // glyphs into diagonal streaks. Restore the caller's state after the upload.
        int previousAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        try {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, width, height, 0,
                    GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, buffer);
        } finally {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, previousAlignment);
        }
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        // Upload happens in the constructor; this texture has no resource-backed source.
    }
}
