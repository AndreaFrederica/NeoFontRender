package neofontrender.addons.loading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import neofontrender.addons.tooltips.AddonI18n;
import org.lwjgl.opengl.GL11;

/** Owns the synchronous language/resource-pack reload session and its framebuffer presentation. */
public enum ResourceReloadRenderer {
    INSTANCE;

    private static final int MATERIAL_GRADIENT_SEGMENTS = 24;

    public enum Operation {
        LANGUAGE,
        RESOURCE_PACKS
    }

    private final ResourceReloadProgress progress = new ResourceReloadProgress();
    private final Arc3DLoadingBarRenderer bar = new Arc3DLoadingBarRenderer();
    private final Arc3DMaterialSpinnerRenderer spinner = new Arc3DMaterialSpinnerRenderer();
    private final ResourceReloadBackdrop backdrop = new ResourceReloadBackdrop();
    private volatile boolean active;
    private Thread ownerThread;
    private Operation operation;
    private String stageKey = "loading.resource_reload.preparing";
    private String detail = "";
    private long lastPulseNanos;
    private boolean pulsing;
    private boolean geometryOnlyFrame;

    public void run(Operation requestedOperation, Runnable reload) {
        if (!enabled(requestedOperation) || active) {
            reload.run();
            return;
        }

        active = true;
        ownerThread = Thread.currentThread();
        operation = requestedOperation;
        stageKey = "loading.resource_reload.preparing";
        detail = "";
        progress.reset();
        bar.reset(System.nanoTime());
        lastPulseNanos = 0L;
        boolean completed = false;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            backdrop.capture();
            mc.loadingScreen.resetProgressAndMessage("");
            // func_73722_d is 1.7.10's displayLoadingString: it also forces one loading frame.
            mc.loadingScreen.func_73722_d("");
            reload.run();
            completed = true;
        } finally {
            if (completed) {
                progress.complete();
                stageKey = "loading.resource_reload.finalizing";
                detail = "";
                requestFrame(true);
            }
            active = false;
            ownerThread = null;
            operation = null;
            backdrop.release();
        }
    }

    public boolean isActive() {
        return active;
    }

    public void beforeProgressStep(String title, int completed, int total, String message) {
        if (!ownsCurrentThread()) return;
        float previous = progress.amount();
        progress.step(title, completed, total);
        if ("Loading Resources".equals(title)) {
            stageKey = "loading.resource_reload.packs";
            detail = clean(message);
        } else if ("Reloading".equals(title)) {
            stageKey = "loading.resource_reload.listeners";
            detail = clean(message);
        } else {
            return;
        }
        if (progress.amount() > previous || completed == 0) requestFrame(false);
    }

    public void progressBarCompleted(String title) {
        if (!ownsCurrentThread()) return;
        float previous = progress.amount();
        progress.completeBar(title);
        if (progress.amount() > previous) {
            stageKey = "loading.resource_reload.finalizing";
            detail = "";
            requestFrame(false);
        }
    }

    public void languageMetadataPhase() {
        if (!ownsCurrentThread()) return;
        progress.languageMetadata();
        stageKey = "loading.resource_reload.languages";
        detail = "";
        requestFrame(false);
    }

    public void rendererRefreshPhase() {
        if (!ownsCurrentThread()) return;
        progress.rendererRefresh();
        stageKey = "loading.resource_reload.renderers";
        detail = "";
        requestFrame(false);
    }

    public void render(int width, int height) {
        if (!active || width <= 0 || height <= 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        long now = System.nanoTime();
        float amount = progress.amount();
        int margin = Math.max(12, Math.min(28, width / 32));
        int bottom = height - Math.max(13, height / 28);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        LoadingBlendMode.enableSourceOver();
        if (!backdrop.draw(width, height)) {
            Gui.drawRect(0, 0, width, height, 0xFF101318);
        }
        Gui.drawRect(0, 0, width, height, 0x28000000);
        drawVerticalGradient(0, height * 3 / 5, width, height,
                0x00000000, 0xC8000000);

        if (!geometryOnlyFrame) {
            String label = title() + dots(now);
            float titleScale = width >= 700 ? 2.25F : width >= 420 ? 1.85F : 1.5F;
            GL11.glPushMatrix();
            GL11.glTranslatef(margin, bottom - font.FONT_HEIGHT * titleScale, 0.0F);
            GL11.glScalef(titleScale, titleScale, 1.0F);
            font.drawString(label, 0, 0, ResourceReloadConfig.textColor, false);
            GL11.glPopMatrix();

            if (!detail.isEmpty()) {
                String visibleDetail = font.trimStringToWidth(detail, Math.max(0, width - margin * 2));
                font.drawString(visibleDetail, margin, bottom + 2, 0xFFB8C0CC, false);
            }
        }

        int spinnerX = width - margin - 10;
        int spinnerY = bottom - 7;
        if (ResourceReloadConfig.spinner) {
            spinner.draw(spinnerX, spinnerY, ResourceReloadConfig.accentColor, 1.0F, now);
        }
        if (ResourceReloadConfig.percentage && !geometryOnlyFrame) {
            String percent = Math.round(amount * 100.0F) + "%";
            int right = ResourceReloadConfig.spinner ? spinnerX - 17 : width - margin;
            font.drawString(percent, right - font.getStringWidth(percent), bottom - font.FONT_HEIGHT - 1,
                    ResourceReloadConfig.textColor, false);
        }
        if (ResourceReloadConfig.progressBar) {
            bar.draw(width, height, amount, ResourceReloadConfig.accentColor, 1.0F, now);
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        LoadingBlendMode.restoreMinecraftDefault();
        GL11.glDisable(GL11.GL_BLEND);
    }

    private String title() {
        String key = "neofontrender_ui_enhancements." + stageKey;
        String translated = AddonI18n.tr(key);
        if (!key.equals(translated)) return translated;
        String operationKey = operation == Operation.LANGUAGE
                ? "neofontrender_ui_enhancements.loading.resource_reload.language"
                : "neofontrender_ui_enhancements.loading.resource_reload.resource_packs";
        String fallback = AddonI18n.tr(operationKey);
        return operationKey.equals(fallback) ? "Reloading resources" : fallback;
    }

    private void requestFrame(boolean force) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.loadingScreen == null) return;
        if (force) mc.loadingScreen.func_73722_d("");
        else mc.loadingScreen.setLoadingProgress(Math.round(progress.amount() * 100.0F));
    }

    /** Presents an animation frame between expensive resource operations on the render thread. */
    public void pulse() {
        if (!ownsCurrentThread() || pulsing) return;
        long now = System.nanoTime();
        if (now - lastPulseNanos < 16_666_667L) return;
        lastPulseNanos = now;
        pulsing = true;
        geometryOnlyFrame = true;
        try {
            requestFrame(false);
        } finally {
            geometryOnlyFrame = false;
            pulsing = false;
        }
    }

    private boolean ownsCurrentThread() {
        return active && Thread.currentThread() == ownerThread;
    }

    private static boolean enabled(Operation operation) {
        if (!ResourceReloadConfig.enabled) return false;
        return operation == Operation.LANGUAGE
                ? ResourceReloadConfig.languageSwitch
                : ResourceReloadConfig.resourcePackSwitch;
    }

    private static String clean(String value) {
        if (value == null) return "";
        String result = value.trim();
        int separator = Math.max(result.lastIndexOf('.'), result.lastIndexOf('$'));
        if (separator >= 0 && separator + 1 < result.length()) result = result.substring(separator + 1);
        return result;
    }

    private static String dots(long now) {
        int count = 1 + (int) ((now / 360_000_000L) % 3L);
        return count == 1 ? "." : count == 2 ? ".." : "...";
    }

    private static void drawVerticalGradient(int left, int top, int right, int bottom,
                                             int topColor, int bottomColor) {
        float startA = (topColor >>> 24) / 255.0F;
        float startR = (topColor >> 16 & 255) / 255.0F;
        float startG = (topColor >> 8 & 255) / 255.0F;
        float startB = (topColor & 255) / 255.0F;
        float endA = (bottomColor >>> 24) / 255.0F;
        float endR = (bottomColor >> 16 & 255) / 255.0F;
        float endG = (bottomColor >> 8 & 255) / 255.0F;
        float endB = (bottomColor & 255) / 255.0F;
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        LoadingBlendMode.enableSourceOver();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        try {
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            for (int segment = 0; segment < MATERIAL_GRADIENT_SEGMENTS; segment++) {
                float firstPosition = segment / (float) MATERIAL_GRADIENT_SEGMENTS;
                float secondPosition = (segment + 1) / (float) MATERIAL_GRADIENT_SEGMENTS;
                float firstAmount = WorldLoadingRenderer.materialGradientCurve(firstPosition);
                float secondAmount = WorldLoadingRenderer.materialGradientCurve(secondPosition);
                float firstY = top + (bottom - top) * firstPosition;
                float secondY = top + (bottom - top) * secondPosition;
                tessellator.setColorRGBA_F(
                        lerp(startR, endR, firstAmount), lerp(startG, endG, firstAmount),
                        lerp(startB, endB, firstAmount), lerp(startA, endA, firstAmount));
                tessellator.addVertex(right, firstY, 0.0D);
                tessellator.addVertex(left, firstY, 0.0D);
                tessellator.setColorRGBA_F(
                        lerp(startR, endR, secondAmount), lerp(startG, endG, secondAmount),
                        lerp(startB, endB, secondAmount), lerp(startA, endA, secondAmount));
                tessellator.addVertex(left, secondY, 0.0D);
                tessellator.addVertex(right, secondY, 0.0D);
            }
            tessellator.draw();
        } finally {
            GL11.glShadeModel(GL11.GL_FLAT);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
        }
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }
}
