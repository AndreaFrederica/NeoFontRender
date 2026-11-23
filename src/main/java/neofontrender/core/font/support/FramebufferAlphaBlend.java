package neofontrender.core.font.support;

import org.lwjgl.opengl.GL11;

/** Alpha factors shared by text draws that may target an offscreen RGBA framebuffer. */
public final class FramebufferAlphaBlend {
    public static final int SOURCE_FACTOR = GL11.GL_ONE;
    public static final int DESTINATION_FACTOR = GL11.GL_ONE_MINUS_SRC_ALPHA;

    private FramebufferAlphaBlend() {
    }

    static float composite(float sourceAlpha, float destinationAlpha) {
        return sourceAlpha + destinationAlpha * (1.0F - sourceAlpha);
    }
}
