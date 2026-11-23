package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import org.lwjgl.opengl.GL11;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Built-in cosmetic effects shared by item and entity previews. */
final class PreviewEffects {
    private static boolean registered;
    private static final Set<String> REPORTED_UNKNOWN = ConcurrentHashMap.newKeySet();
    private static final Set<String> REPORTED_FAILURES = ConcurrentHashMap.newKeySet();

    private PreviewEffects() {}

    static synchronized void registerBuiltins() {
        if (registered) return;
        registered = true;
        register("shimmer", PreviewEffects::shimmer);
        register("pulse_frame", PreviewEffects::pulseFrame);
        register("ray_glow", (request, size) -> new NfrTooltipApi.PreviewInsets(8, 8, 8, 8),
                PreviewEffects::rayGlow);
        register("icon_particles", PreviewEffects::particleOutsets, PreviewEffects::iconParticles);
        register("inward_particles", PreviewEffects::particleOutsets,
                context -> particles(context, true));
        register("line_particles", (request, size) -> TooltipConfig.previewParticlesEnabled
                        ? new NfrTooltipApi.PreviewInsets(8, 0, 13, 0)
                        : NfrTooltipApi.PreviewInsets.NONE,
                PreviewEffects::lineParticles);
    }

    static void render(NfrTooltipApi.PreviewRequest request, int x, int y,
                       NfrTooltipApi.PreviewSize size, float appearance) {
        if (!TooltipConfig.previewEffectsEnabled || request == null || request.effects().isEmpty()) return;
        NfrTooltipApi.PreviewEffectContext context = new NfrTooltipApi.PreviewEffectContext(
                request, x, y, size, appearance, System.nanoTime());
        for (String id : request.effects()) {
            if (id == null) continue;
            NfrTooltipApi.PreviewEffect effect = NfrTooltipApi.PreviewRegistry.findEffect(id);
            if (effect == null) {
                reportUnknown(id);
                continue;
            }
            try { effect.render(context); }
            catch (RuntimeException | LinkageError error) { reportFailure(id, "render", error); }
        }
    }

    static NfrTooltipApi.PreviewInsets outsets(NfrTooltipApi.PreviewRequest request,
                                                NfrTooltipApi.PreviewSize size) {
        if (!TooltipConfig.previewEffectsEnabled || request == null || request.effects().isEmpty()) {
            return NfrTooltipApi.PreviewInsets.NONE;
        }
        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;
        for (String id : request.effects()) {
            if (id == null) continue;
            NfrTooltipApi.PreviewEffect effect = NfrTooltipApi.PreviewRegistry.findEffect(id);
            if (effect == null) {
                reportUnknown(id);
                continue;
            }
            try {
                NfrTooltipApi.PreviewInsets value = effect.outsets(request, size);
                if (value == null) continue;
                left = Math.max(left, value.left());
                top = Math.max(top, value.top());
                right = Math.max(right, value.right());
                bottom = Math.max(bottom, value.bottom());
            } catch (RuntimeException | LinkageError error) {
                reportFailure(id, "measure", error);
            }
        }
        return left == 0 && top == 0 && right == 0 && bottom == 0
                ? NfrTooltipApi.PreviewInsets.NONE
                : new NfrTooltipApi.PreviewInsets(left, top, right, bottom);
    }

    private static void register(String id, EffectRenderer renderer) {
        register(id, (request, size) -> NfrTooltipApi.PreviewInsets.NONE, renderer);
    }

    private static void register(String id, OutsetProvider outsets, EffectRenderer renderer) {
        NfrTooltipApi.PreviewRegistry.registerEffect(new NfrTooltipApi.PreviewEffect() {
            @Override public String id() { return id; }
            @Override public NfrTooltipApi.PreviewInsets outsets(
                    NfrTooltipApi.PreviewRequest request, NfrTooltipApi.PreviewSize size) {
                return outsets.get(request, size);
            }
            @Override public void render(NfrTooltipApi.PreviewEffectContext context) {
                renderer.render(context);
            }
        });
    }

    private static NfrTooltipApi.PreviewInsets particleOutsets(
            NfrTooltipApi.PreviewRequest request, NfrTooltipApi.PreviewSize size) {
        return TooltipConfig.previewParticlesEnabled
                ? new NfrTooltipApi.PreviewInsets(2, 2, 2, 2)
                : NfrTooltipApi.PreviewInsets.NONE;
    }

    private static void reportUnknown(String id) {
        String key = id.trim().toLowerCase(java.util.Locale.ROOT);
        if (REPORTED_UNKNOWN.add(key)) {
            NfrUiEnhancements.LOGGER.warn("Unknown tooltip preview effect '{}'", id);
        }
    }

    private static void reportFailure(String id, String operation, Throwable error) {
        String key = id + ':' + operation + ':' + error.getClass().getName();
        if (REPORTED_FAILURES.add(key)) {
            NfrUiEnhancements.LOGGER.warn("Tooltip preview effect '{}' failed during {}",
                    id, operation, error);
        }
    }

    private static void shimmer(NfrTooltipApi.PreviewEffectContext context) {
        float phase = phase(context, 1600L);
        float bandHeight = Math.max(2.0F, context.size().height() * 0.16F);
        float bandY = context.y() + Math.max(0.0F,
                context.size().height() - bandHeight) * phase;
        withOverlay(() -> {
            drawQuad(context.x(), bandY, context.x() + context.size().width(), bandY + bandHeight,
                    color(0xB8FFFFFF, 0.20F * context.appearance()), color(0x00FFFFFF, 0.0F));
        });
    }

    private static void pulseFrame(NfrTooltipApi.PreviewEffectContext context) {
        float pulse = 0.55F + 0.45F * (float) Math.sin(context.timeNanos() / 320_000_000.0D
                * TooltipConfig.previewEffectSpeed);
        int alpha = Math.round(135.0F * context.appearance() * pulse);
        withOverlay(() -> {
            GL11.glLineWidth(1.25F);
            GlStateManager.color(0.50F, 0.90F, 1.0F, alpha / 255.0F);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(context.x() + 1.0F, context.y() + 1.0F);
            GL11.glVertex2f(context.x() + context.size().width() - 1.0F, context.y() + 1.0F);
            GL11.glVertex2f(context.x() + context.size().width() - 1.0F,
                    context.y() + context.size().height() - 1.0F);
            GL11.glVertex2f(context.x() + 1.0F, context.y() + context.size().height() - 1.0F);
            GL11.glEnd();
        });
    }

    private static void rayGlow(NfrTooltipApi.PreviewEffectContext context) {
        float pulse = 0.5F + 0.5F * (float) Math.sin(context.timeNanos() / 450_000_000.0D
                * TooltipConfig.previewEffectSpeed);
        int alpha = Math.round(75.0F * context.appearance() * pulse);
        float cx = context.x() + context.size().width() * 0.5F;
        float cy = context.y() + context.size().height() * 0.5F;
        float extent = 4.0F + 4.0F * pulse;
        withOverlay(() -> {
            drawQuad(cx - 0.5F, context.y() - extent, cx + 0.5F, context.y() + 2.0F,
                    color(0x00CFFFFF, 0.0F), color(0x80CFFFFF, alpha / 255.0F));
            drawQuad(cx - 0.5F, context.y() + context.size().height() - 2.0F,
                    cx + 0.5F, context.y() + context.size().height() + extent,
                    color(0x80CFFFFF, alpha / 255.0F), color(0x00CFFFFF, 0.0F));
            drawHorizontalQuad(context.x() - extent, cy - 0.5F,
                    context.x() + 2.0F, cy + 0.5F,
                    color(0x00CFFFFF, 0.0F), color(0x80CFFFFF, alpha / 255.0F));
            drawHorizontalQuad(context.x() + context.size().width() - 2.0F, cy - 0.5F,
                    context.x() + context.size().width() + extent, cy + 0.5F,
                    color(0x80CFFFFF, alpha / 255.0F), color(0x00CFFFFF, 0.0F));
        });
    }

    private static void particles(NfrTooltipApi.PreviewEffectContext context, boolean inward) {
        if (!TooltipConfig.previewParticlesEnabled || TooltipConfig.previewParticleCount <= 0) return;
        int count = Math.min(32, TooltipConfig.previewParticleCount);
        long tick = (long) (context.timeNanos() / 50_000_000L
                * TooltipConfig.previewEffectSpeed);
        withOverlay(() -> {
            for (int index = 0; index < count; index++) {
                float seed = index * 17.0F + 3.0F;
                float life = ((tick + index * 11L) % 36L) / 36.0F;
                float x;
                float y;
                if (inward) {
                    float edgeX = (float) Math.abs(Math.sin(seed * 1.7F));
                    float edgeY = (float) Math.abs(Math.cos(seed * 0.9F));
                    x = context.x() + context.size().width() * (edgeX * (1.0F - life) + 0.5F * life);
                    y = context.y() + context.size().height() * (edgeY * (1.0F - life) + 0.5F * life);
                } else {
                    x = context.x() + context.size().width() * (float) Math.abs(Math.sin(seed + life * 2.0F));
                    y = context.y() + context.size().height() * (float) Math.abs(Math.cos(seed * 0.7F + life * 2.0F));
                }
                float alpha = context.appearance() * (1.0F - life) * 0.85F;
                float radius = 0.8F + 0.8F * (1.0F - life);
                drawQuad(x - radius, y - radius, x + radius, y + radius,
                        color(0xB8D7FFFF, alpha), color(0x00B8E5FF, 0.0F));
            }
        });
    }

    private static void iconParticles(NfrTooltipApi.PreviewEffectContext context) {
        if (!TooltipConfig.previewParticlesEnabled || TooltipConfig.previewParticleCount <= 0) return;
        ItemStack stack = previewStack(context.request());
        if (stack.isEmpty()) return;
        int count = Math.min(16, TooltipConfig.previewParticleCount);
        long tick = (long) (context.timeNanos() / 50_000_000L
                * TooltipConfig.previewEffectSpeed);
        RenderItem renderer = Minecraft.getMinecraft().getRenderItem();
        for (int index = 0; index < count; index++) {
            float seed = index * 17.0F + 3.0F;
            float life = ((tick + index * 11L) % 42L) / 42.0F;
            float px = context.x() + context.size().width()
                    * (float) Math.abs(Math.sin(seed + life * 2.0F));
            float py = context.y() + context.size().height()
                    * (float) Math.abs(Math.cos(seed * 0.7F + life * 2.0F));
            float alpha = context.appearance() * (1.0F - life);
            float scale = (0.13F + 0.10F * (1.0F - life))
                    * Math.max(0.15F, context.appearance());
            drawItemParticle(renderer, stack, px, py, scale, alpha,
                    (seed * 13.0F + life * 90.0F) % 360.0F);
        }
    }

    private static ItemStack previewStack(NfrTooltipApi.PreviewRequest request) {
        if (request instanceof NfrTooltipApi.ItemPreviewRequest) {
            return ((NfrTooltipApi.ItemPreviewRequest) request).stack();
        }
        if (request instanceof NfrTooltipApi.ArmorPreviewRequest) {
            return ((NfrTooltipApi.ArmorPreviewRequest) request).stack();
        }
        return ItemStack.EMPTY;
    }

    private static void drawItemParticle(RenderItem renderer, ItemStack stack, float x, float y,
                                         float scale, float alpha, float rotation) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.pushMatrix();
        float oldZ = renderer.zLevel;
        try {
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.translate(x, y, 650.0F);
            GlStateManager.rotate(rotation, 0.0F, 0.0F, 1.0F);
            GlStateManager.scale(scale, scale, 1.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            RenderHelper.enableGUIStandardItemLighting();
            renderer.zLevel = 650.0F;
            renderer.renderItemAndEffectIntoGUI(stack, -8, -8);
        } finally {
            renderer.zLevel = oldZ;
            RenderHelper.disableStandardItemLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
            GL11.glPopAttrib();
        }
    }

    private static void lineParticles(NfrTooltipApi.PreviewEffectContext context) {
        if (!TooltipConfig.previewParticlesEnabled) return;
        float phase = phase(context, 1100L);
        withOverlay(() -> {
            for (int index = 0; index < 3; index++) {
                float y = context.y() + context.size().height() * (0.25F + index * 0.25F);
                float start = context.x() - 8.0F + (context.size().width() + 16.0F)
                        * ((phase + index * 0.31F) % 1.0F);
                drawQuad(start, y, start + 5.0F, y + 0.8F,
                        color(0xB8FFFFFF, context.appearance() * 0.65F),
                        color(0x00FFFFFF, 0.0F));
            }
        });
    }

    private static float phase(NfrTooltipApi.PreviewEffectContext context, long periodMillis) {
        double period = Math.max(1.0D, periodMillis * 1_000_000.0D
                / Math.max(0.1F, TooltipConfig.previewEffectSpeed));
        return (float) ((context.timeNanos() % (long) period) / period);
    }

    private static int color(int rgbWithAlphaHint, float alpha) {
        return (rgbWithAlphaHint & 0x00FFFFFF)
                | (Math.max(0, Math.min(255, Math.round(alpha * 255.0F))) << 24);
    }

    private static void withOverlay(Runnable draw) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.disableTexture2D();
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            draw.run();
        } finally {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
            GL11.glPopAttrib();
        }
    }

    private static void drawQuad(float left, float top, float right, float bottom,
                                 int startColor, int endColor) {
        drawQuad(left, top, right, bottom,
                startColor, startColor, endColor, endColor);
    }

    private static void drawHorizontalQuad(float left, float top, float right, float bottom,
                                           int startColor, int endColor) {
        drawQuad(left, top, right, bottom,
                startColor, endColor, endColor, startColor);
    }

    private static void drawQuad(float left, float top, float right, float bottom,
                                 int topLeft, int topRight, int bottomRight, int bottomLeft) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, left, top, topLeft);
        vertex(buffer, right, top, topRight);
        vertex(buffer, right, bottom, bottomRight);
        vertex(buffer, left, bottom, bottomLeft);
        tessellator.draw();
    }

    private static void vertex(BufferBuilder buffer, float x, float y, int color) {
        buffer.pos(x, y, 0.0D).color((color >> 16) & 255, (color >> 8) & 255,
                color & 255, (color >>> 24) & 255).endVertex();
    }

    private interface EffectRenderer {
        void render(NfrTooltipApi.PreviewEffectContext context);
    }

    private interface OutsetProvider {
        NfrTooltipApi.PreviewInsets get(NfrTooltipApi.PreviewRequest request,
                                        NfrTooltipApi.PreviewSize size);
    }
}
