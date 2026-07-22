package neofontrender.addons.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import neofontrender.addons.mixin.AccessorShaderGroup;
import neofontrender.addons.ui.NfrUiEnhancements;
import org.apache.logging.log4j.Level;

public enum ScreenEffectsRenderer {
    INSTANCE;

    private static final ResourceLocation BLUR = new ResourceLocation(
            NfrUiEnhancements.MOD_ID, "shaders/post/ui_blur.json");
    private long openedNanos;
    private boolean ownsShader;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        openedNanos = System.nanoTime();
        Minecraft mc = Minecraft.getMinecraft();
        if (event.getGui() == null || mc.world == null || !ScreenEffectsConfig.enabled || !ScreenEffectsConfig.blur) {
            releaseShader(mc);
            return;
        }
        installShader(mc);
    }

    public boolean drawBackground(GuiScreen screen) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ScreenEffectsConfig.enabled || mc.world == null || (!ScreenEffectsConfig.blur && !ScreenEffectsConfig.gradient)) {
            return false;
        }
        if (ScreenEffectsConfig.blur && ownsShader && mc.entityRenderer.getShaderGroup() == null) installShader(mc);
        if (ScreenEffectsConfig.gradient) drawGradient(screen.width, screen.height, fadeProgress());
        return true;
    }

    /** GuiChat does not call drawWorldBackground, so advance blur independently of screen drawing. */
    @SubscribeEvent
    public void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !ownsShader) return;
        Minecraft mc = Minecraft.getMinecraft();
        ShaderGroup group = mc.entityRenderer.getShaderGroup();
        if (mc.currentScreen != null && group != null) updateRadius(group, ScreenEffectsConfig.blurRadius * fadeProgress());
    }

    public void configChanged() {
        Minecraft mc = Minecraft.getMinecraft();
        openedNanos = System.nanoTime();
        if (mc.currentScreen == null || mc.world == null || !ScreenEffectsConfig.enabled || !ScreenEffectsConfig.blur) {
            releaseShader(mc);
        } else {
            if (ownsShader) releaseShader(mc);
            installShader(mc);
        }
    }

    private void installShader(Minecraft mc) {
        if (ownsShader && mc.entityRenderer.getShaderGroup() != null) {
            updateRadius(mc.entityRenderer.getShaderGroup(), ScreenEffectsConfig.blurRadius * fadeProgress());
            return;
        }
        if (mc.entityRenderer.isShaderActive()) return;
        mc.entityRenderer.loadShader(BLUR);
        ShaderGroup group = mc.entityRenderer.getShaderGroup();
        ownsShader = group != null;
        if (ownsShader) updateRadius(group, ScreenEffectsConfig.blurRadius * fadeProgress());
    }

    private void updateRadius(ShaderGroup group, float amount) {
        try {
            for (Shader pass : ((AccessorShaderGroup) group).nfrUi$getShaders()) {
                ShaderUniform radius = pass.getShaderManager().getShaderUniform("Radius");
                if (radius != null) radius.set(amount);
            }
        } catch (Throwable throwable) {
            NfrUiEnhancements.LOGGER.log(Level.WARN, "Could not update UI blur radius", throwable);
        }
    }

    private void releaseShader(Minecraft mc) {
        if (ownsShader) mc.entityRenderer.stopUseShader();
        ownsShader = false;
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
