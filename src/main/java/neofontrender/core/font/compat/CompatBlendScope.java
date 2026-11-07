package neofontrender.core.font.compat;

import net.minecraft.client.renderer.GlStateManager;
import neofontrender.core.font.support.FramebufferAlphaBlend;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

/**
 * Fixed-function state scope for one compatibility glyph draw.
 *
 * <p>Compatibility pages use the legacy texture environment to turn a single-channel coverage
 * texture into the current text colour. That state is global to the current OpenGL context, so
 * it must be restored together with the texture unit and binding. SmoothFont has the same
 * requirement and snapshots every combine parameter rather than restoring only the mode.</p>
 */
public final class CompatBlendScope implements AutoCloseable {

    private static final int[] TEX_ENV_PNAMES = {
            GL11.GL_TEXTURE_ENV_MODE,
            GL13.GL_COMBINE_RGB,
            GL13.GL_SOURCE0_RGB,
            GL13.GL_OPERAND0_RGB,
            GL13.GL_COMBINE_ALPHA,
            GL13.GL_SOURCE0_ALPHA,
            GL13.GL_OPERAND0_ALPHA,
            GL13.GL_SOURCE1_ALPHA,
            GL13.GL_OPERAND1_ALPHA
    };

    private final boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
    private final boolean alphaTestEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
    private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
    private final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
    private final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int activeTexture;
    private final boolean activeTextureEnabled;
    private final int activeTextureBinding;
    private final boolean texture0Enabled;
    private final int texture0Binding;
    private final int[] texture0Env = new int[TEX_ENV_PNAMES.length];

    public CompatBlendScope() {
        // Compat always renders on unit 0. The caller may have left the lightmap or another
        // auxiliary unit active; binding a page on that unit is the direct cause of large texture
        // fragments appearing in later GUI quads.
        activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        activeTextureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        activeTextureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        selectTextureUnit(GL13.GL_TEXTURE0);
        texture0Enabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        texture0Binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        for (int index = 0; index < TEX_ENV_PNAMES.length; index++) {
            texture0Env[index] = GL11.glGetTexEnvi(GL11.GL_TEXTURE_ENV, TEX_ENV_PNAMES[index]);
        }

        GlStateManager.enableTexture2D();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GlStateManager.disableAlpha();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GlStateManager.enableBlend();
        GL11.glEnable(GL11.GL_BLEND);
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                FramebufferAlphaBlend.SOURCE_FACTOR, FramebufferAlphaBlend.DESTINATION_FACTOR);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
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
        // Restore the exact texture environment on unit 0 before restoring the caller's unit.
        selectTextureUnit(GL13.GL_TEXTURE0);
        for (int index = 0; index < TEX_ENV_PNAMES.length; index++) {
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, TEX_ENV_PNAMES[index], texture0Env[index]);
        }

        GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        if (blendEnabled) {
            GlStateManager.enableBlend();
            GL11.glEnable(GL11.GL_BLEND);
        } else {
            GlStateManager.disableBlend();
            GL11.glDisable(GL11.GL_BLEND);
        }
        if (alphaTestEnabled) {
            GlStateManager.enableAlpha();
            GL11.glEnable(GL11.GL_ALPHA_TEST);
        } else {
            GlStateManager.disableAlpha();
            GL11.glDisable(GL11.GL_ALPHA_TEST);
        }

        restoreTextureUnit(GL13.GL_TEXTURE0, texture0Enabled, texture0Binding);
        if (activeTexture != GL13.GL_TEXTURE0) {
            restoreTextureUnit(activeTexture, activeTextureEnabled, activeTextureBinding);
        }
        selectTextureUnit(activeTexture);
    }

    private static void restoreTextureUnit(int unit, boolean enabled, int binding) {
        selectTextureUnit(unit);
        if (enabled) {
            GlStateManager.enableTexture2D();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        } else {
            GlStateManager.disableTexture2D();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }
        GlStateManager.bindTexture(binding);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, binding);
    }

    private static void selectTextureUnit(int unit) {
        GlStateManager.setActiveTexture(unit);
        GL13.glActiveTexture(unit);
    }
}
