package neofontrender.core.font.compat;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.IResourceManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A one-byte coverage page uploaded in a core-profile-compatible texture format.
 *
 * <p>The mask is single-channel. Depending on {@link CompatPageFormat} it is uploaded as the
 * legacy {@code GL_ALPHA} format or as {@code GL_R8} with texture swizzle. In both cases RGB
 * comes from the primary vertex colour through the texture-environment combine, so the page only
 * carries coverage. One byte per texel is what makes higher oversampling practical for a
 * 256-page CJK working set.</p>
 *
 * <p>The page is owned by {@link CompatTextureManager} and is deliberately never registered with
 * Minecraft's {@code TextureManager}: that map deletes and recreates names by number, and a glyph
 * page must never be able to take part in vanilla's panorama/background id churn.</p>
 */
final class CompatPageTexture extends AbstractTexture {

    /** Core-profile sized single-channel format; not exposed by LWJGL 2's GL11. */
    private static final int GL_R8 = 0x8229;
    private static final int GL_RED = 0x1903;
    private static final int GL_TEXTURE_SWIZZLE_R = 0x8E42;
    private static final int GL_TEXTURE_SWIZZLE_G = 0x8E43;
    private static final int GL_TEXTURE_SWIZZLE_B = 0x8E44;
    private static final int GL_TEXTURE_SWIZZLE_A = 0x8E45;

    CompatPageTexture(byte[] mask, int side, boolean linear, CompatPageFormat format) {
        GlStateManager.bindTexture(getGlTextureId());
        int filter = linear ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        ByteBuffer buffer = BufferUtils.createByteBuffer(side * side);
        buffer.put(mask);
        buffer.flip();
        if (format == CompatPageFormat.R8_SWIZZLE) {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            try {
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL_R8, side, side, 0,
                        GL_RED, GL11.GL_UNSIGNED_BYTE, buffer);
            } finally {
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
            }
            // Sampling returns (1,1,1,mask) so the legacy combine keeps the text colour and takes
            // only coverage from the page. Driver-side swizzle applies to any sampler that reads
            // the texture, including the fixed-function emulation used by core-profile shims.
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_R, GL11.GL_ONE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_G, GL11.GL_ONE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_B, GL11.GL_ONE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_A, GL_RED);
        } else {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_ALPHA, side, side, 0,
                    GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, buffer);
        }
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        // The texture is uploaded eagerly by the constructor.
    }
}
