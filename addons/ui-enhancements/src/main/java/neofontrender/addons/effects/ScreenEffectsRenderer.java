package neofontrender.addons.effects;

import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.mixin.AccessorShaderGroup;
import neofontrender.addons.ui.NfrUiEnhancements;
import org.apache.logging.log4j.Level;

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
        if (event.getGui() == null || !ScreenEffectsConfig.enabled || !ScreenEffectsConfig.blur) {
            discardShader();
        }
    }

    /** Runs after the world (including Kirino's finalizer) and before any GUI pixels are submitted. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeScreenDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.getGui() == null || mc.world == null || !ScreenEffectsConfig.enabled) return;

        float progress = fadeProgress();
        if (ScreenEffectsConfig.blur) renderBlur(mc);
        if (ScreenEffectsConfig.gradient) drawGradient(event.getGui().width, event.getGui().height, progress);
    }

    /** Cancel the opaque vanilla dirt/dim background; the replacement was already drawn in Pre. */
    public boolean drawBackground(GuiScreen screen) {
        Minecraft mc = Minecraft.getMinecraft();
        return ScreenEffectsConfig.enabled && mc.world != null
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

    private void renderBlur(Minecraft mc) {
        ShaderGroup group = ensureShader(mc);
        if (group == null) {
            restoreGuiTarget(mc);
            return;
        }

        try {
            updateRadius(group, animatedRadius());
            group.render(mc.getRenderPartialTicks());
        } catch (Throwable throwable) {
            NfrUiEnhancements.LOGGER.log(Level.WARN,
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
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private ShaderGroup ensureShader(Minecraft mc) {
        if (shaderCreationFailed) return null;
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
            NfrUiEnhancements.LOGGER.log(Level.WARN,
                    "Could not create the private UI blur shader; blur is disabled until resource reload", exception);
            discardShader();
            return null;
        }
    }

    private void updateRadius(ShaderGroup group, float amount) {
        for (Shader pass : ((AccessorShaderGroup) group).nfrUi$getShaders()) {
            ShaderUniform radius = pass.getShaderManager().getShaderUniform("Radius");
            if (radius != null) radius.set(amount);
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
        int[] c = ScreenEffectsConfig.colors;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, width, 0, c[1], alphaScale);
        vertex(buffer, 0, 0, c[0], alphaScale);
        vertex(buffer, 0, height, c[3], alphaScale);
        vertex(buffer, width, height, c[2], alphaScale);
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private static void vertex(BufferBuilder buffer, int x, int y, int color, float alphaScale) {
        int alpha = Math.round((color >>> 24) * alphaScale);
        buffer.pos(x, y, 0.0D).color(color >> 16 & 255, color >> 8 & 255, color & 255, alpha).endVertex();
    }
}
