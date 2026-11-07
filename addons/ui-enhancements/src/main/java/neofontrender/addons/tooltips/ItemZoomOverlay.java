package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.build.UiBuildFeatures;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * Item Zoom style inventory overlay for the 1.12 GUI renderer.
 *
 * Every tooltip path supplies its hovered stack. Draw before the tooltip or after the GUI
 * according to the configured overlap order, at most once per frame across all entry paths.
 */
final class ItemZoomOverlay {
    private static final long CAPTURE_TIMEOUT_NANOS = 500_000_000L;
    private static final long ROTATION_WINDOW_NANOS = 3_600_000_000_000L;
    private static final ItemZoomDrawOrder DRAW_ORDER = new ItemZoomDrawOrder();

    private static ItemStack captured = ItemStack.EMPTY;
    private static GuiScreen capturedScreen;
    private static long capturedAt;
    private static ItemStack hovered = ItemStack.EMPTY;
    private static GuiScreen hoveredScreen;
    private static boolean hoveredThisFrame;

    static ItemStack hoveredStack(GuiScreen screen) {
        return screen == hoveredScreen ? hovered : ItemStack.EMPTY;
    }

    static void reset() {
        captured = ItemStack.EMPTY;
        capturedScreen = null;
        hovered = ItemStack.EMPTY;
        hoveredScreen = null;
        hoveredThisFrame = false;
        DRAW_ORDER.beginFrame();
        ItemZoomRenderTarget.release();
        PreviewBoundsMeasurement.releaseTargets();
        TooltipPreviewRenderers.resetItemZoomAnimation();
        ItemZoomRenderer.clearModelCache();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beginFrame(GuiScreenEvent.DrawScreenEvent.Pre event) {
        DRAW_ORDER.beginFrame();
        captured = ItemStack.EMPTY;
        capturedScreen = null;
        hoveredThisFrame = false;
    }

    /** Standard Forge tooltip source. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void capture(RenderTooltipEvent.Pre event) {
        beforeTooltip(event.getStack(), event.getX(), event.getY());
    }

    /** Also called by renderers that bypass Forge's Pre event, before any tooltip pixels. */
    static void beforeTooltip(ItemStack stack, int mouseX, int mouseY) {
        capture(stack);
        drawCaptured(Minecraft.getMinecraft().currentScreen, mouseX, mouseY,
                ItemZoomDrawOrder.Phase.BEFORE_TOOLTIP);
    }

    /** Shared source used by the modern document renderer and HEI bridge. */
    static void capture(ItemStack stack) {
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiScreen screen = minecraft.currentScreen;
        if (!(screen instanceof GuiContainer) || stack == null || stack.isEmpty()) return;
        hovered = stack.copy();
        hoveredScreen = screen;
        hoveredThisFrame = true;
        if (!ItemZoomKeyBindings.isActive() || !allowed(stack)
                || "off".equals(ItemZoomPresentation.mode(ItemZoomPresentation.category(stack)))) return;
        captured = stack.copy();
        capturedScreen = screen;
        capturedAt = System.nanoTime();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void draw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!hoveredThisFrame || event.getGui() != hoveredScreen) {
            hovered = ItemStack.EMPTY;
            hoveredScreen = null;
        }
        hoveredThisFrame = false;
        drawCaptured(event.getGui(), event.getMouseX(), event.getMouseY(),
                ItemZoomDrawOrder.Phase.AFTER_SCREEN);
        captured = ItemStack.EMPTY;
        capturedScreen = null;
    }

    private static void drawCaptured(GuiScreen screen, int mouseX, int mouseY,
                                     ItemZoomDrawOrder.Phase phase) {
        if (!ItemZoomKeyBindings.isActive()) {
            TooltipPreviewRenderers.resetItemZoomAnimation();
            captured = ItemStack.EMPTY;
            capturedScreen = null;
            return;
        }
        if (captured.isEmpty()
                || screen != capturedScreen
                || System.nanoTime() - capturedAt > CAPTURE_TIMEOUT_NANOS) {
            return;
        }
        // Claim before drawing: a model renderer can re-enter a tooltip path.
        if (!DRAW_ORDER.claim(TooltipConfig.zoomOverlayLayer, phase)) return;

        ItemStack stack = captured;
        captured = ItemStack.EMPTY;
        capturedScreen = null;
        try {
            render(screen, stack, mouseX, mouseY);
            TooltipPreviewRenderers.itemZoomRendered();
        } catch (RuntimeException | LinkageError failure) {
            if (UiBuildFeatures.DIAGNOSTIC_LOGS) {
                TooltipModule.LOGGER.warn("Item zoom overlay render failed for {}",
                        stack.getItem().getRegistryName(), failure);
            }
        }
    }

    private static void render(GuiScreen screen, ItemStack stack, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft);
        int screenWidth = resolution.getScaledWidth();
        int screenHeight = resolution.getScaledHeight();
        int gap = Math.max(2, TooltipConfig.zoomOverlayGap);
        if (!(screen instanceof GuiContainer)) return;

        GuiContainer container = (GuiContainer) screen;
        int panelInset = TooltipConfig.zoomOverlayPanel
                ? Math.max(0, TooltipConfig.zoomOverlayPanelInset) : 0;
        ItemZoomLayout layout = ItemZoomLayout.fit(TooltipConfig.zoomOverlaySide, mouseX,
                screenWidth, screenHeight, container.getGuiLeft(), container.getGuiTop(),
                container.getXSize(), container.getYSize(), TooltipConfig.zoomOverlaySize,
                gap, panelInset);
        if (layout == null) return;
        int x = layout.x;
        int y = layout.y;
        int renderSize = layout.size;
        float appear = TooltipPreviewRenderers.itemZoomAnimationProgress(stack);
        if (appear <= 0.001F) return;

        int panelLeft = x - panelInset;
        int panelTop = y - panelInset;
        int panelRight = x + renderSize + panelInset;
        int panelBottom = y + renderSize + panelInset;

        // Item Zoom 1.12 changes fixed-function lighting while drawing. Capture and restore the
        // complete caller state, including GlStateManager's cached color, so lighting or fade
        // alpha cannot leak into the rest of the GUI.
        try (ModernTooltipRenderer.CallerGlState ignored = ModernTooltipRenderer.CallerGlState.capture()) {
            // Tooltip callbacks may inherit a widget transform or scissor. The standalone
            // preview always uses screen coordinates, then restores the caller's matrices.
            int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0, resolution.getScaledWidth_double(),
                    resolution.getScaledHeight_double(), 0, 1000, 3000);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.translate(0, 0, -2000);
            RenderItem renderer = minecraft.getRenderItem();
            float oldZ = renderer == null ? 0.0F : renderer.zLevel;
            try {
                GL20.glUseProgram(0);
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableFog();
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.disableDepth();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                if (TooltipConfig.zoomOverlayPanel) {
                    drawPanel(panelLeft, panelTop, panelRight, panelBottom);
                }
                if (renderer == null) return;

                FontRenderer font = minecraft.fontRenderer;
                float scale = renderSize / 16.0F;
                ItemZoomPresentation.Category category = ItemZoomPresentation.category(stack);
                String mode = ItemZoomPresentation.mode(category);
                if ("off".equals(mode)) return;
                if (ItemZoomRenderTarget.render(renderSize, resolution.getScaleFactor(),
                        () -> ItemZoomRenderer.render(stack, category, mode, renderSize))) {
                    ItemZoomRenderTarget.composite(x, y, renderSize, appear);
                } else {
                    // If framebuffer support is disabled, retain a vanilla GUI-icon fallback.
                    GlStateManager.pushMatrix();
                    try {
                        GlStateManager.translate(x, y, 0);
                        ItemZoomRenderer.renderIcon(stack, renderSize);
                    } finally {
                        GlStateManager.popMatrix();
                    }
                }

                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableDepth();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.translate(x, y, 0.0F);
                    GlStateManager.scale(scale, scale, 1.0F);
                    String amount = TooltipConfig.zoomOverlayShowStackSize && stack.getCount() > 1
                            ? Integer.toString(stack.getCount()) : "";
                    if (TooltipConfig.zoomOverlayShowDurability) {
                        renderer.renderItemOverlayIntoGUI(font, stack, 0, 0, amount);
                    } else if (!amount.isEmpty()) {
                        font.drawStringWithShadow(amount, 17 - font.getStringWidth(amount), 8,
                                0xFFFFFFFF);
                    }
                    if (TooltipConfig.zoomOverlayShowCooldown && minecraft.player != null) {
                        float cooldown = minecraft.player.getCooldownTracker().getCooldown(
                                stack.getItem(), minecraft.getRenderPartialTicks());
                        if (cooldown > 0.0F) {
                            int top = Math.max(0, Math.round(16.0F * (1.0F - cooldown)));
                            Gui.drawRect(0, top, 16, 16, 0xAA000000);
                        }
                    }
                } finally {
                    GlStateManager.popMatrix();
                }
            } finally {
                if (renderer != null) renderer.zLevel = oldZ;
                RenderHelper.disableStandardItemLighting();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(matrixMode);
            }
        }
    }

    private static void drawPanel(int left, int top, int right, int bottom) {
        int fill = TooltipConfig.zoomOverlayBackgroundColor;
        int border = TooltipConfig.zoomOverlayBorderColor;
        Gui.drawRect(left, top, right, bottom, fill);
        if (TooltipConfig.zoomOverlayBorder) {
            Gui.drawRect(left, top, right, top + 1, border);
            Gui.drawRect(left, bottom - 1, right, bottom, border);
            Gui.drawRect(left, top, left + 1, bottom, border);
            Gui.drawRect(right - 1, top, right, bottom, border);
        }
    }

    static boolean allowed(ItemStack stack) {
        String id = stack.getItem().getRegistryName() == null ? ""
                : stack.getItem().getRegistryName().toString();
        if (matches(TooltipConfig.zoomOverlayBlacklist, id)) return false;
        if (matches(TooltipConfig.zoomOverlayWhitelist, id)) return true;
        return ItemZoomPresentation.inScope(ItemZoomPresentation.category(stack), TooltipConfig.zoomOverlayScope);
    }

    private static boolean matches(java.util.List<String> rules, String id) {
        if (rules == null) return false;
        for (String rule : rules) {
            if (rule == null) continue;
            String value = rule.trim().toLowerCase(java.util.Locale.ROOT);
            if (value.isEmpty()) continue;
            if (value.endsWith(":*")) {
                if (id.toLowerCase(java.util.Locale.ROOT).startsWith(
                        value.substring(0, value.length() - 1))) return true;
            } else if (value.equals(id.toLowerCase(java.util.Locale.ROOT))) return true;
        }
        return false;
    }

    static float rotationAngle(long timeNanos, float degreesPerSecond) {
        if (!Float.isFinite(degreesPerSecond) || degreesPerSecond == 0.0F) return 0.0F;
        long cycle = Math.floorMod(timeNanos, ROTATION_WINDOW_NANOS);
        return (float) ((cycle / 1_000_000_000.0D * degreesPerSecond) % 360.0D);
    }

}
