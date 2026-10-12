package neofontrender.addons.tooltips;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import neofontrender.addons.build.UiBuildFeatures;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.List;
import java.util.Locale;

/**
 * Item Zoom style inventory overlay for the 1.7.10 GUI renderer.
 *
 * Every tooltip path supplies its hovered stack. Draw before the tooltip or after the GUI
 * according to the configured overlap order, at most once per frame across all entry paths.
 */
public final class ItemZoomOverlay {
    private static final long CAPTURE_TIMEOUT_NANOS = 500_000_000L;
    private static final long ROTATION_WINDOW_NANOS = 3_600_000_000_000L;
    private static final ItemZoomDrawOrder DRAW_ORDER = new ItemZoomDrawOrder();

    // 1.7.10 has no ItemStack.EMPTY; an absent stack is null.
    private static ItemStack captured;
    private static GuiScreen capturedScreen;
    private static long capturedAt;
    private static ItemStack hovered;
    private static GuiScreen hoveredScreen;
    private static boolean hoveredThisFrame;

    static ItemStack hoveredStack(GuiScreen screen) {
        return screen == hoveredScreen ? hovered : null;
    }

    static void reset() {
        captured = null;
        capturedScreen = null;
        hovered = null;
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
        captured = null;
        capturedScreen = null;
        hoveredThisFrame = false;
    }

    /** Shared by the modern tooltip renderer, the HEI bridge and the document renderer. */
    static void beforeTooltip(ItemStack stack, int mouseX, int mouseY) {
        capture(stack);
        drawCaptured(Minecraft.getMinecraft().currentScreen, mouseX, mouseY,
                ItemZoomDrawOrder.Phase.BEFORE_TOOLTIP);
    }

    static void capture(ItemStack stack) {
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiScreen screen = minecraft.currentScreen;
        if (!(screen instanceof GuiContainer) || stack == null) return;
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
        if (!hoveredThisFrame || event.gui != hoveredScreen) {
            hovered = null;
            hoveredScreen = null;
        }
        hoveredThisFrame = false;
        drawCaptured(event.gui, event.mouseX, event.mouseY,
                ItemZoomDrawOrder.Phase.AFTER_SCREEN);
        captured = null;
        capturedScreen = null;
    }

    private static void drawCaptured(GuiScreen screen, int mouseX, int mouseY,
                                     ItemZoomDrawOrder.Phase phase) {
        if (!ItemZoomKeyBindings.isActive()) {
            TooltipPreviewRenderers.resetItemZoomAnimation();
            captured = null;
            capturedScreen = null;
            return;
        }
        if (captured == null
                || screen != capturedScreen
                || System.nanoTime() - capturedAt > CAPTURE_TIMEOUT_NANOS) {
            return;
        }
        // Claim before drawing: a model renderer can re-enter a tooltip path.
        if (!DRAW_ORDER.claim(TooltipConfig.zoomOverlayLayer, phase)) return;

        ItemStack stack = captured;
        captured = null;
        capturedScreen = null;
        try {
            render(screen, stack, mouseX, mouseY);
            TooltipPreviewRenderers.itemZoomRendered();
        } catch (RuntimeException | LinkageError failure) {
            if (UiBuildFeatures.DIAGNOSTIC_LOGS) {
                TooltipModule.LOGGER.warn("Item zoom overlay render failed for {}",
                        Item.itemRegistry.getNameForObject(stack.getItem()), failure);
            }
        }
    }

    private static void render(GuiScreen screen, ItemStack stack, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(
                minecraft, minecraft.displayWidth, minecraft.displayHeight);
        int screenWidth = resolution.getScaledWidth();
        int screenHeight = resolution.getScaledHeight();
        int gap = Math.max(2, TooltipConfig.zoomOverlayGap);
        if (!(screen instanceof GuiContainer)) return;

        int panelInset = TooltipConfig.zoomOverlayPanel
                ? Math.max(0, TooltipConfig.zoomOverlayPanelInset) : 0;
        // 1.7.10 keeps GuiContainer's frame (guiLeft/guiTop/xSize/ySize) in protected fields with
        // no public accessor and this addon holds no access transformer for them, so the cursor
        // stands in for the container frame and the surface is centred in the free screen half.
        ItemZoomLayout layout = ItemZoomLayout.fit(TooltipConfig.zoomOverlaySide, mouseX,
                screenWidth, screenHeight, mouseX, mouseY, 0, 0, TooltipConfig.zoomOverlaySize,
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
        // complete caller state so lighting or fade alpha cannot leak into the rest of the GUI.
        try (ModernTooltipRenderer.CallerGlState ignored = ModernTooltipRenderer.CallerGlState.capture()) {
            // Tooltip callbacks may inherit a widget transform or scissor. The standalone
            // preview always uses screen coordinates, then restores the caller's matrices.
            int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, resolution.getScaledWidth_double(),
                    resolution.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
            RenderItem renderer = RenderItem.getInstance();
            float oldZ = renderer == null ? 0.0F : renderer.zLevel;
            try {
                GL20.glUseProgram(0);
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                RenderHelper.disableStandardItemLighting();
                GL11.glDisable(GL11.GL_FOG);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
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
                    GL11.glPushMatrix();
                    try {
                        GL11.glTranslatef(x, y, 0.0F);
                        ItemZoomRenderer.renderIcon(stack, renderSize);
                    } finally {
                        GL11.glPopMatrix();
                    }
                }

                RenderHelper.disableStandardItemLighting();
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glPushMatrix();
                try {
                    GL11.glTranslatef(x, y, 0.0F);
                    GL11.glScalef(scale, scale, 1.0F);
                    String amount = TooltipConfig.zoomOverlayShowStackSize && stack.stackSize > 1
                            ? Integer.toString(stack.stackSize) : "";
                    if (TooltipConfig.zoomOverlayShowDurability) {
                        renderer.renderItemOverlayIntoGUI(
                                font, minecraft.getTextureManager(), stack, 0, 0, amount);
                    } else if (!amount.isEmpty()) {
                        font.drawStringWithShadow(amount, 17 - font.getStringWidth(amount), 8,
                                0xFFFFFFFF);
                    }
                    // 1.7.10 has no per-item cooldown tracker, so the cooldown sweep that main
                    // draws over the slot has no source data on this platform.
                } finally {
                    GL11.glPopMatrix();
                }
            } finally {
                if (renderer != null) renderer.zLevel = oldZ;
                RenderHelper.disableStandardItemLighting();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(matrixMode);
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
        if (stack == null) return false;
        String name = Item.itemRegistry.getNameForObject(stack.getItem());
        String id = name == null ? "" : name;
        if (matches(TooltipConfig.zoomOverlayBlacklist, id)) return false;
        if (matches(TooltipConfig.zoomOverlayWhitelist, id)) return true;
        return ItemZoomPresentation.inScope(ItemZoomPresentation.category(stack), TooltipConfig.zoomOverlayScope);
    }

    private static boolean matches(List<String> rules, String id) {
        if (rules == null) return false;
        for (String rule : rules) {
            if (rule == null) continue;
            String value = rule.trim().toLowerCase(Locale.ROOT);
            if (value.isEmpty()) continue;
            if (value.endsWith(":*")) {
                if (id.toLowerCase(Locale.ROOT).startsWith(
                        value.substring(0, value.length() - 1))) return true;
            } else if (value.equals(id.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    static float rotationAngle(long timeNanos, float degreesPerSecond) {
        if (!Float.isFinite(degreesPerSecond) || degreesPerSecond == 0.0F) return 0.0F;
        long cycle = Math.floorMod(timeNanos, ROTATION_WINDOW_NANOS);
        return (float) ((cycle / 1_000_000_000.0D * degreesPerSecond) % 360.0D);
    }

}
