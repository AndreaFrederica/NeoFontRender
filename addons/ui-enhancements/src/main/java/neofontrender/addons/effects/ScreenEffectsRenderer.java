package neofontrender.addons.effects;

import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.mixin.AccessorShaderGroup;
import neofontrender.addons.ui.NfrUiEnhancements;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

/**
 * Renders in-world screen effects without taking ownership of EntityRenderer's global ShaderGroup.
 *
 * <p>Cleanroom's Kirino renderer finalizes the world into Minecraft's framebuffer immediately before
 * the GUI is drawn. Running our private post chain from DrawScreenEvent.Pre therefore consumes the
 * completed frame and cannot race Kirino's HDR/ping-pong framebuffers or another mod's entity shader.</p>
 */
public enum ScreenEffectsRenderer implements IResourceManagerReloadListener {
    INSTANCE;

    private static final ResourceLocation BLUR = new ResourceLocation(
            NfrUiEnhancements.MOD_ID, "shaders/post/ui_blur.json");

    private long openedNanos;
    private ShaderGroup blurGroup;
    private int framebufferWidth = -1;
    private int framebufferHeight = -1;
    private boolean shaderCreationFailed;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        openedNanos = System.nanoTime();
        if (event.gui == null || !ScreenEffectsConfig.enabled || !ScreenEffectsConfig.blur) {
            discardShader();
        }
    }

    /** Runs after the world (including Kirino's finalizer) and before any GUI pixels are submitted. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeScreenDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.gui == null || mc.theWorld == null || !ScreenEffectsConfig.enabled) return;

        float progress = fadeProgress();
        if (ScreenEffectsConfig.blur) renderBlur(mc, event.renderPartialTicks);
        if (ScreenEffectsConfig.gradient) drawGradient(event.gui.width, event.gui.height, progress);
    }

    /** Cancel the opaque vanilla dirt/dim background; the replacement was already drawn in Pre. */
    public boolean drawBackground(GuiScreen screen) {
        Minecraft mc = Minecraft.getMinecraft();
        return ScreenEffectsConfig.enabled && mc.theWorld != null
                && (ScreenEffectsConfig.blur || ScreenEffectsConfig.gradient);
    }

    public void configChanged() {
        openedNanos = System.nanoTime();
        shaderCreationFailed = false;
        if (!ScreenEffectsConfig.enabled || !ScreenEffectsConfig.blur) discardShader();
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        shaderCreationFailed = false;
        discardShader();
    }

    private void renderBlur(Minecraft mc, float partialTicks) {
        ShaderGroup group = ensureShader(mc);
        if (group == null) {
            restoreGuiTarget(mc);
            return;
        }

        try {
            updateRadius(group, animatedRadius());
            group.loadShaderGroup(partialTicks);
        } catch (Throwable throwable) {
            NfrUiEnhancements.LOGGER.warn(
                    "UI blur pass failed; disabling it until the next resource reload", throwable);
            shaderCreationFailed = true;
            discardShader();
        } finally {
            restoreGuiTarget(mc);
        }
    }

    private static void restoreGuiTarget(Minecraft mc) {
        // ShaderGroup leaves its last output bound and changes the projection matrices. The GUI
        // event expects Minecraft's main target and the standard scaled overlay projection.
        mc.getFramebuffer().bindFramebuffer(true);
        mc.entityRenderer.setupOverlayRendering();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private ShaderGroup ensureShader(Minecraft mc) {
        if (shaderCreationFailed || !OpenGlHelper.shadersSupported) return null;
        int width = mc.displayWidth;
        int height = mc.displayHeight;
        if (blurGroup != null && width == framebufferWidth && height == framebufferHeight) return blurGroup;

        discardShader();
        try {
            blurGroup = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), BLUR);
            blurGroup.createBindFramebuffers(width, height);
            framebufferWidth = width;
            framebufferHeight = height;
            return blurGroup;
        } catch (IOException | JsonSyntaxException exception) {
            shaderCreationFailed = true;
            NfrUiEnhancements.LOGGER.warn(
                    "Could not create the private UI blur shader; blur is disabled until resource reload", exception);
            discardShader();
            return null;
        }
    }

    private void updateRadius(ShaderGroup group, float amount) {
        for (Shader pass : ((AccessorShaderGroup) group).nfrUi$getShaders()) {
            ShaderUniform radius = pass.getShaderManager().func_147991_a("Radius");
            if (radius != null) radius.func_148090_a(amount);
        }
    }

    private float animatedRadius() {
        // Minecraft 1.12's built-in blur kernel is undefined at radius zero and can output a
        // flat grey framebuffer. Radius 1 is its neutral, valid starting point.
        return 1.0F + Math.max(0.0F, ScreenEffectsConfig.blurRadius - 1.0F) * fadeProgress();
    }

    private void discardShader() {
        if (blurGroup != null) {
            blurGroup.deleteShaderGroup();
            blurGroup = null;
        }
        framebufferWidth = -1;
        framebufferHeight = -1;
    }

    private float fadeProgress() {
        if (!ScreenEffectsConfig.fade || ScreenEffectsConfig.fadeDurationMillis <= 0) return 1.0F;
        float p = Math.min((System.nanoTime() - openedNanos) /
                (ScreenEffectsConfig.fadeDurationMillis * 1_000_000.0F), 1.0F);
        return 1.0F - (1.0F - p) * (1.0F - p);
    }

    private static void drawGradient(int width, int height, float alphaScale) {
        int[] colors = ScreenEffectsConfig.colors;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        vertex(tessellator, width, 0, colors[1], alphaScale);
        vertex(tessellator, 0, 0, colors[0], alphaScale);
        vertex(tessellator, 0, height, colors[3], alphaScale);
        vertex(tessellator, width, height, colors[2], alphaScale);
        tessellator.draw();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static void vertex(Tessellator tessellator, int x, int y, int color, float alphaScale) {
        int alpha = Math.round((color >>> 24) * alphaScale);
        tessellator.setColorRGBA(color >> 16 & 255, color >> 8 & 255, color & 255, alpha);
        tessellator.addVertex(x, y, 0.0D);
    }
}
