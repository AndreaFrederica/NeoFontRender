package neofontrender.core.font.compat;

import net.minecraft.client.renderer.GlStateManager;
import neofontrender.core.font.support.FramebufferAlphaBlend;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

/**
 * Compatibility text needs the same explicit straight-alpha state as the modern renderers.
 *
 * <p>Vanilla bitmap glyphs are effectively 1-bit alpha, so they tolerate whatever blend/alpha-test
 * state the calling screen left behind. TTF pages have real coverage, so inheriting, for example,
 * an enabled alpha test or an additive blend turns soft edges into hard/dark fringes. This scope
 * mirrors {@code AwtModernTextRenderer.StraightAlphaBlendState}: disable alpha test, enable straight
 * alpha blending, then restore the caller's exact state.</p>
 *
 * <p>Pages are single-channel {@code GL_ALPHA} textures. The texture environment combines RGB from
 * the primary colour (the current vanilla text colour) with alpha from the page, exactly like
 * SmoothFont's grayscale pages.</p>
 */
public final class CompatBlendScope implements AutoCloseable {

    private final boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
    private final boolean alphaTestEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
    private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
    private final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
    private final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int textureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    private final int texEnvMode = GL11.glGetTexEnvi(
            GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE);

    public CompatBlendScope() {
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                FramebufferAlphaBlend.SOURCE_FACTOR, FramebufferAlphaBlend.DESTINATION_FACTOR);

        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL11.GL_REPLACE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL13.GL_PRIMARY_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_ALPHA, GL11.GL_MODULATE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_ALPHA, GL11.GL_TEXTURE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE1_ALPHA, GL13.GL_PRIMARY_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND1_ALPHA, GL11.GL_SRC_ALPHA);
    }

    @Override
    public void close() {
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, texEnvMode);
        GlStateManager.bindTexture(textureBinding);
        GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        if (!blendEnabled) {
            GlStateManager.disableBlend();
        }
        if (alphaTestEnabled) {
            GlStateManager.enableAlpha();
        } else {
            GlStateManager.disableAlpha();
        }
    }
}
