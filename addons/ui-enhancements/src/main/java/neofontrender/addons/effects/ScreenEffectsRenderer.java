package neofontrender.addons.effects;

import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.mixin.AccessorShaderGroup;
import neofontrender.addons.ui.NfrUiEnhancements;
import org.apache.logging.log4j.Level;

import java.io.IOException;

/**
 * Renders in-world screen effects without taking ownership of EntityRenderer's global ShaderGroup.
 *
 * <p>The blur chain runs after Cleanroom/Kirino and EntityRenderer's global post chain have finalized
 * the world into Minecraft's main framebuffer, but before overlay projection, item activation, HUD,
 * and screen rendering. The gradient is drawn after the HUD, which also lets it remain visible while
 * a closing transition finishes after the current screen has gone away.</p>
 */
public enum ScreenEffectsRenderer implements IResourceManagerReloadListener {
    INSTANCE;

    private static final ResourceLocation BLUR = new ResourceLocation(
            NfrUiEnhancements.MOD_ID, "shaders/post/ui_blur.json");

    private final Transition overlayTransition = new Transition();
    private final Transition blurTransition = new Transition();
    private ScreenProfile currentProfile = ScreenProfile.NONE;
    private ShaderGroup blurGroup;
    private int framebufferWidth = -1;
    private int framebufferHeight = -1;
    private boolean shaderCreationFailed;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            snapHidden();
            discardShader();
            return;
        }
        applyProfile(profileFor(event.getGui()));
    }

    /** Draws the dimming layer after the HUD, including during the close transition. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return;

        float progress = overlayTransition.progress(System.nanoTime());
        if (progress <= 0.0F) return;
        drawGradient(event.getResolution().getScaledWidth(),
                event.getResolution().getScaledHeight(), progress);
    }

    /**
     * Invoked from EntityRenderer at the vanilla post-processing boundary. This must remain before
     * setupOverlayRendering so the private chain sees only the completed world framebuffer.
     */
    public void renderBeforeOverlay(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            discardShader();
            return;
        }

        float progress = blurTransition.progress(System.nanoTime());
        if (progress <= 0.0F) {
            if (!blurTransition.targetsVisible()) discardShader();
            return;
        }
        if (!OpenGlHelper.shadersSupported) return;
        renderBlur(mc, partialTicks, progress);
    }

    /** Cancel the opaque vanilla dirt/dim background; the replacement was already drawn in Pre. */
    public boolean drawBackground(GuiScreen screen) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return false;
        ScreenProfile profile = profileFor(screen);
        return profile.overlay
                || (profile.blur && OpenGlHelper.shadersSupported && !shaderCreationFailed);
    }

    public void configChanged() {
        shaderCreationFailed = false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            snapHidden();
            discardShader();
            return;
        }
        applyProfile(profileFor(mc.currentScreen));
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        shaderCreationFailed = false;
        discardShader();
    }

    private void renderBlur(Minecraft mc, float partialTicks, float progress) {
        ShaderGroup group = ensureShader(mc);
        if (group == null) {
            restoreMainTarget(mc);
            return;
        }

        try {
            updateParameters(group, effectiveBlurRadius(ScreenEffectsConfig.blurRadius), progress);
            group.render(partialTicks);
        } catch (Throwable throwable) {
            NfrUiEnhancements.LOGGER.log(Level.WARN,
                    "UI blur pass failed; disabling it until the next resource reload", throwable);
            shaderCreationFailed = true;
            discardShader();
        } finally {
            restoreMainTarget(mc);
        }
    }

    private static void restoreMainTarget(Minecraft mc) {
        // EntityRenderer performs the normal overlay projection and state setup immediately after
        // this hook. ShaderManager leaves the active texture at its final sampler unit; our
        // composite has two samplers, so explicitly return to unit zero before HUD textures bind.
        mc.getFramebuffer().bindFramebuffer(true);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.bindTexture(0);
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

    private void updateParameters(ShaderGroup group, float radiusAmount, float progressAmount) {
        for (Shader pass : ((AccessorShaderGroup) group).nfrUi$getShaders()) {
            ShaderUniform radius = pass.getShaderManager().getShaderUniform("Radius");
            if (radius != null) radius.set(radiusAmount);
            ShaderUniform progress = pass.getShaderManager().getShaderUniform("Progress");
            if (progress != null) progress.set(progressAmount);
        }
    }

    static int effectiveBlurRadius(int configuredRadius) {
        // The vanilla GLSL loop changes its sample count discontinuously for fractional radii.
        // Supplying an integer keeps the number of taps and its normalization denominator equal.
        return Math.max(1, Math.min(16, configuredRadius));
    }

    private void discardShader() {
        if (blurGroup != null) {
            blurGroup.deleteShaderGroup();
            blurGroup = null;
        }
        framebufferWidth = -1;
        framebufferHeight = -1;
    }

    private static ScreenProfile profileFor(GuiScreen screen) {
        if (screen == null || !ScreenEffectsConfig.enabled) return ScreenProfile.NONE;
        // The world-loading module owns its backdrop and transition independently.
        if (screen instanceof GuiDownloadTerrain) return ScreenProfile.NONE;

        if (screen instanceof GuiChat) {
            return new ScreenProfile(
                    ScreenEffectsConfig.gradient && ScreenEffectsConfig.gradientChat,
                    ScreenEffectsConfig.blur && ScreenEffectsConfig.blurChat,
                    ScreenEffectsConfig.fade && ScreenEffectsConfig.fadeChat);
        }
        if (screen instanceof GuiContainer) {
            return new ScreenProfile(
                    ScreenEffectsConfig.gradient && ScreenEffectsConfig.gradientContainers,
                    ScreenEffectsConfig.blur && ScreenEffectsConfig.blurContainers,
                    ScreenEffectsConfig.fade && ScreenEffectsConfig.fadeContainers);
        }
        return new ScreenProfile(
                ScreenEffectsConfig.gradient && ScreenEffectsConfig.gradientMenus,
                ScreenEffectsConfig.blur && ScreenEffectsConfig.blurMenus,
                ScreenEffectsConfig.fade && ScreenEffectsConfig.fadeMenus);
    }

    private void applyProfile(ScreenProfile next) {
        long now = System.nanoTime();
        overlayTransition.setTarget(next.overlay,
                next.overlay ? next.animate : currentProfile.animate,
                ScreenEffectsConfig.fadeDurationMillis, now);
        blurTransition.setTarget(next.blur,
                next.blur ? next.animate : currentProfile.animate,
                ScreenEffectsConfig.fadeDurationMillis, now);
        currentProfile = next;
    }

    private void snapHidden() {
        overlayTransition.snap(false);
        blurTransition.snap(false);
        currentProfile = ScreenProfile.NONE;
    }

    private static final class ScreenProfile {
        private static final ScreenProfile NONE = new ScreenProfile(false, false, false);
        private final boolean overlay;
        private final boolean blur;
        private final boolean animate;

        private ScreenProfile(boolean overlay, boolean blur, boolean animate) {
            this.overlay = overlay;
            this.blur = blur;
            this.animate = animate;
        }
    }

    private static final class Transition {
        private float startProgress;
        private float targetProgress;
        private long startedNanos;
        private long durationNanos;

        private void setTarget(boolean visible, boolean animate, int durationMillis, long now) {
            float desired = visible ? 1.0F : 0.0F;
            float current = progress(now);
            if (!animate || durationMillis <= 0) {
                snap(visible);
                return;
            }
            if (targetProgress == desired) return;

            startProgress = current;
            targetProgress = desired;
            startedNanos = now;
            durationNanos = Math.max(1L, Math.round(
                    durationMillis * 1_000_000.0D * Math.abs(desired - current)));
        }

        private void snap(boolean visible) {
            float progress = visible ? 1.0F : 0.0F;
            startProgress = progress;
            targetProgress = progress;
            startedNanos = System.nanoTime();
            durationNanos = 0L;
        }

        private boolean targetsVisible() {
            return targetProgress > 0.0F;
        }

        private float progress(long now) {
            if (durationNanos <= 0L) return targetProgress;
            float elapsed = (float) (now - startedNanos) / durationNanos;
            if (elapsed >= 1.0F) return targetProgress;
            if (elapsed <= 0.0F) return startProgress;
            float eased = elapsed * elapsed * (3.0F - 2.0F * elapsed);
            return startProgress + (targetProgress - startProgress) * eased;
        }
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
