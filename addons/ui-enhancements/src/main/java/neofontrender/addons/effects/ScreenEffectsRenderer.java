package neofontrender.addons.effects;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import neofontrender.addons.mixin.AccessorShaderGroup;
import neofontrender.addons.ui.NfrUiEnhancements;
import org.lwjgl.opengl.GL11;

/** Owns the addon's shader group and background overlay render state. */
public enum ScreenEffectsRenderer {
    INSTANCE;

    private static final ResourceLocation BLUR = new ResourceLocation(
            NfrUiEnhancements.MOD_ID, "shaders/post/ui_blur.json");

    private long openedNanos;
    private boolean ownsShader;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        openedNanos = System.nanoTime();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.gui == null || minecraft.theWorld == null
                || !ScreenEffectsConfig.enabled || !ScreenEffectsConfig.blur) {
            releaseShader(minecraft);
            return;
        }
        installShader(minecraft);
    }

    public boolean drawBackground(GuiScreen screen) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!ScreenEffectsConfig.enabled || minecraft.theWorld == null
                || (!ScreenEffectsConfig.blur && !ScreenEffectsConfig.gradient)) return false;
        if (ScreenEffectsConfig.blur && ownsShader && minecraft.entityRenderer.getShaderGroup() == null) {
            installShader(minecraft);
        }
        if (ScreenEffectsConfig.gradient) drawGradient(screen.width, screen.height, fadeProgress());
        return true;
    }

    @SubscribeEvent
    public void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !ownsShader) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen == null || minecraft.theWorld == null
                || !ScreenEffectsConfig.enabled || !ScreenEffectsConfig.blur) {
            releaseShader(minecraft);
            return;
        }
        ShaderGroup group = minecraft.entityRenderer.getShaderGroup();
        if (group != null) updateRadius(group, animatedRadius());
    }

    private void installShader(Minecraft minecraft) {
        if (!OpenGlHelper.shadersSupported) return;
        if (ownsShader && minecraft.entityRenderer.getShaderGroup() != null) {
            updateRadius(minecraft.entityRenderer.getShaderGroup(), animatedRadius());
            return;
        }
        if (minecraft.entityRenderer.isShaderActive()) return;
        try {
            ShaderGroup group = new ShaderGroup(
                    minecraft.getTextureManager(),
                    minecraft.getResourceManager(),
                    minecraft.getFramebuffer(),
                    BLUR);
            group.createBindFramebuffers(minecraft.displayWidth, minecraft.displayHeight);
            minecraft.entityRenderer.theShaderGroup = group;
            ownsShader = true;
            updateRadius(group, animatedRadius());
        } catch (Exception exception) {
            ownsShader = false;
            NfrUiEnhancements.LOGGER.error("Could not install the UI blur shader", exception);
        }
    }

    private void updateRadius(ShaderGroup group, float amount) {
        for (Shader pass : ((AccessorShaderGroup) group).nfrUi$getShaders()) {
            ShaderUniform radius = pass.getShaderManager().func_147991_a("Radius");
            if (radius != null) radius.func_148090_a(amount);
        }
    }

    private float animatedRadius() {
        return 1.0F + Math.max(0.0F, ScreenEffectsConfig.blurRadius - 1.0F) * fadeProgress();
    }

    private void releaseShader(Minecraft minecraft) {
        if (ownsShader) minecraft.entityRenderer.deactivateShader();
        ownsShader = false;
    }

    private float fadeProgress() {
        if (!ScreenEffectsConfig.fade || ScreenEffectsConfig.fadeDurationMillis <= 0) return 1.0F;
        float progress = Math.min((System.nanoTime() - openedNanos)
                / (ScreenEffectsConfig.fadeDurationMillis * 1_000_000.0F), 1.0F);
        return 1.0F - (1.0F - progress) * (1.0F - progress);
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
