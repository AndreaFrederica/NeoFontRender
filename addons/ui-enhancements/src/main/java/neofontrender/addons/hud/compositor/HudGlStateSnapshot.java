package neofontrender.addons.hud.compositor;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * A small compatibility boundary for legacy immediate-mode HUD renderers.
 *
 * Some 1.12-era mods render an entity inside a HUD and leave the driver state
 * on a lightmap texture unit, with lighting/depth or an altered blend state.
 * Minecraft's GlStateManager cache does not see raw GL changes made by those
 * mods, so restoring only through one side is insufficient.
 */
public final class HudGlStateSnapshot implements AutoCloseable {
    private final boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
    private final boolean light0 = GL11.glIsEnabled(GL11.GL_LIGHT0);
    private final boolean light1 = GL11.glIsEnabled(GL11.GL_LIGHT1);
    private final boolean colorMaterial = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
    private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
    private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
    private final boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
    private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    private final boolean fog = GL11.glIsEnabled(GL11.GL_FOG);
    private final boolean rescaleNormal = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
    private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
    private final int alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
    private final float alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
    private final int cullFace = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
    private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
    private final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
    private final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int blendEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
    private final int blendEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
    private final int shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
    private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final TextureUnitState[] textureUnits = readTextureUnits(activeTexture);
    private final float[] color = readColor();
    private final boolean[] colorMask = readColorMask();
    private boolean closed;

    private HudGlStateSnapshot() {
        // This also covers scissor, fog, lighting and depth attributes that are
        // easy for a third-party renderer to change without updating the cache.
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    }

    public static HudGlStateSnapshot capture() {
        return new HudGlStateSnapshot();
    }

    /** Establish the state expected by textured, alpha-blended 2D HUD code. */
    public static void prepareHud2d() {
        GlStateManager.setActiveTexture(OpenGlHelperCompat.DEFAULT_TEXTURE_UNIT);
        GL13.glActiveTexture(OpenGlHelperCompat.DEFAULT_TEXTURE_UNIT);
        GlStateManager.enableTexture2D();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GlStateManager.enableAlpha();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GlStateManager.enableBlend();
        GL11.glEnable(GL11.GL_BLEND);
        GlStateManager.disableLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GlStateManager.disableDepth();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.depthMask(false);
        GL11.glDepthMask(false);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        GL11.glPopAttrib();

        GL20.glUseProgram(program);
        GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
        GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        restoreToggle(lighting, GL11.GL_LIGHTING,
                GlStateManager::enableLighting, GlStateManager::disableLighting);
        restoreToggle(light0, GL11.GL_LIGHT0,
                () -> GlStateManager.enableLight(0), () -> GlStateManager.disableLight(0));
        restoreToggle(light1, GL11.GL_LIGHT1,
                () -> GlStateManager.enableLight(1), () -> GlStateManager.disableLight(1));
        restoreToggle(colorMaterial, GL11.GL_COLOR_MATERIAL,
                GlStateManager::enableColorMaterial, GlStateManager::disableColorMaterial);
        restoreToggle(depth, GL11.GL_DEPTH_TEST,
                GlStateManager::enableDepth, GlStateManager::disableDepth);
        GlStateManager.depthMask(depthMask);
        GL11.glDepthMask(depthMask);
        GlStateManager.depthFunc(depthFunc);
        GL11.glDepthFunc(depthFunc);
        GlStateManager.alphaFunc(alphaFunc, alphaRef);
        GL11.glAlphaFunc(alphaFunc, alphaRef);
        GlStateManager.cullFace(cullFace == GL11.GL_FRONT
                ? GlStateManager.CullFace.FRONT
                : cullFace == GL11.GL_FRONT_AND_BACK
                ? GlStateManager.CullFace.FRONT_AND_BACK
                : GlStateManager.CullFace.BACK);
        GL11.glCullFace(cullFace);
        restoreToggle(blend, GL11.GL_BLEND,
                GlStateManager::enableBlend, GlStateManager::disableBlend);
        restoreToggle(alpha, GL11.GL_ALPHA_TEST,
                GlStateManager::enableAlpha, GlStateManager::disableAlpha);
        restoreToggle(cull, GL11.GL_CULL_FACE,
                GlStateManager::enableCull, GlStateManager::disableCull);
        restoreToggle(fog, GL11.GL_FOG,
                GlStateManager::enableFog, GlStateManager::disableFog);
        restoreToggle(rescaleNormal, GL12.GL_RESCALE_NORMAL,
                GlStateManager::enableRescaleNormal, GlStateManager::disableRescaleNormal);
        GlStateManager.shadeModel(shadeModel);
        GL11.glShadeModel(shadeModel);
        GlStateManager.colorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
        GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
        GlStateManager.color(color[0], color[1], color[2], color[3]);
        GL11.glColor4f(color[0], color[1], color[2], color[3]);
        restoreTextureUnits(textureUnits, activeTexture);
    }

    private static void restoreToggle(boolean enabled, int capability,
                                      Runnable enable, Runnable disable) {
        if (enabled) enable.run();
        else disable.run();
        if (enabled) GL11.glEnable(capability);
        else GL11.glDisable(capability);
    }

    private static float[] readColor() {
        FloatBuffer values = BufferUtils.createFloatBuffer(4);
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, values);
        return new float[]{values.get(0), values.get(1), values.get(2), values.get(3)};
    }

    private static boolean[] readColorMask() {
        IntBuffer values = BufferUtils.createIntBuffer(4);
        GL11.glGetInteger(GL11.GL_COLOR_WRITEMASK, values);
        return new boolean[]{values.get(0) != 0, values.get(1) != 0,
                values.get(2) != 0, values.get(3) != 0};
    }

    private static TextureUnitState[] readTextureUnits(int originalActiveTexture) {
        TextureUnitState[] states = new TextureUnitState[3];
        for (int i = 0; i < states.length; i++) {
            int unit = GL13.GL_TEXTURE0 + i;
            GL13.glActiveTexture(unit);
            states[i] = new TextureUnitState(unit, GL11.glIsEnabled(GL11.GL_TEXTURE_2D),
                    GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
        }
        GL13.glActiveTexture(originalActiveTexture);
        return states;
    }

    private static void restoreTextureUnits(TextureUnitState[] states, int originalActiveTexture) {
        for (TextureUnitState state : states) {
            GlStateManager.setActiveTexture(state.unit);
            GL13.glActiveTexture(state.unit);
            restoreToggle(state.enabled, GL11.GL_TEXTURE_2D,
                    GlStateManager::enableTexture2D, GlStateManager::disableTexture2D);
            GlStateManager.bindTexture(state.binding);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.binding);
        }
        GlStateManager.setActiveTexture(originalActiveTexture);
        GL13.glActiveTexture(originalActiveTexture);
    }

    private static final class TextureUnitState {
        private final int unit;
        private final boolean enabled;
        private final int binding;

        private TextureUnitState(int unit, boolean enabled, int binding) {
            this.unit = unit;
            this.enabled = enabled;
            this.binding = binding;
        }
    }

    /** Avoids loading OpenGlHelper in this utility's static initialization. */
    private static final class OpenGlHelperCompat {
        private static final int DEFAULT_TEXTURE_UNIT =
                net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit;
    }
}
