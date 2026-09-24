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
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.build.UiBuildFeatures;

/**
 * Item Zoom style inventory overlay for the 1.12 GUI renderer.
 *
 * The stack is supplied by every tooltip path and the actual draw is deferred until the GUI
 * post event. The deferred draw is important for HEI and Modern Tooltip, which can replace
 * the normal tooltip renderer without publishing a Forge RenderTooltipEvent.Pre themselves.
 */
final class ItemZoomOverlay {
    private static final long CAPTURE_TIMEOUT_NANOS = 500_000_000L;
    private static final long ROTATION_WINDOW_NANOS = 3_600_000_000_000L;

    private static ItemStack captured = ItemStack.EMPTY;
    private static GuiScreen capturedScreen;
    private static long capturedAt;

    /** Standard Forge tooltip source. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void capture(RenderTooltipEvent.Pre event) {
        capture(event.getStack());
    }

    /** Shared source used by the modern document renderer and HEI bridge. */
    static void capture(ItemStack stack) {
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiScreen screen = minecraft.currentScreen;
        if (!TooltipConfig.zoomOverlayEnabled || !(screen instanceof GuiContainer)
                || stack == null || stack.isEmpty() || !allowed(stack)) {
            return;
        }
        captured = stack.copy();
        capturedScreen = screen;
        capturedAt = System.nanoTime();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void draw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!TooltipConfig.zoomOverlayEnabled || captured.isEmpty()
                || event.getGui() != capturedScreen
                || System.nanoTime() - capturedAt > CAPTURE_TIMEOUT_NANOS) {
            return;
        }

        ItemStack stack = captured;
        captured = ItemStack.EMPTY;
        capturedScreen = null;
        try {
            render(event.getGui(), stack, event.getMouseX(), event.getMouseY());
        } catch (RuntimeException | LinkageError failure) {
            if (UiBuildFeatures.DIAGNOSTIC_LOGS) {
                TooltipModule.LOGGER.warn("Item zoom overlay render failed for {}",
                        stack.getItem().getRegistryName(), failure);
            }
        }
    }

    private void render(GuiScreen screen, ItemStack stack, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft);
        int screenWidth = resolution.getScaledWidth();
        int screenHeight = resolution.getScaledHeight();
        int size = Math.max(32, Math.min(TooltipConfig.zoomOverlaySize, 160));
        int gap = Math.max(2, TooltipConfig.zoomOverlayGap);
        if (!(screen instanceof GuiContainer)) return;

        Bounds area = availableArea((GuiContainer) screen, mouseX, screenWidth, screenHeight, gap);
        int renderSize = Math.min(size, Math.min(area.width - gap * 2, area.height - gap * 2));
        if (renderSize < 32) return;

        int x = area.left + (area.width - renderSize) / 2;
        int y = area.top + (area.height - renderSize) / 2;
        float appear = TooltipPreviewRenderers.itemZoomAnimationProgress(stack);
        if (appear <= 0.001F) return;

        int panelInset = TooltipConfig.zoomOverlayPanel
                ? Math.max(0, TooltipConfig.zoomOverlayPanelInset) : 0;
        int panelLeft = x - panelInset;
        int panelTop = y - panelInset;
        int panelRight = x + renderSize + panelInset;
        int panelBottom = y + renderSize + panelInset;

        // Item Zoom 1.12 changes fixed-function lighting while drawing. Capture and restore the
        // complete caller state, including GlStateManager's cached color, so lighting or fade
        // alpha cannot leak into the rest of the GUI.
        try (ModernTooltipRenderer.CallerGlState ignored = ModernTooltipRenderer.CallerGlState.capture()) {
            GlStateManager.pushMatrix();
            RenderItem renderer = minecraft.getRenderItem();
            float oldZ = renderer == null ? 0.0F : renderer.zLevel;
            try {
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
                float centerX = x + renderSize * 0.5F;
                float centerY = y + renderSize * 0.5F;
                float rotation = TooltipConfig.zoomOverlayRotation
                        ? rotationAngle(System.nanoTime(), TooltipConfig.zoomOverlayRotationSpeed) : 0.0F;
                float scale = renderSize / 16.0F;

                GlStateManager.color(1.0F, 1.0F, 1.0F, appear);
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.translate(centerX, centerY, 0.0F);
                    // Rotate in the GUI plane. Rotating a flat item around Y makes it edge-on
                    // and intermittently invisible, which the original 1.12 renderer avoids.
                    GlStateManager.rotate(rotation, 0.0F, 0.0F, 1.0F);
                    // Item Zoom only enlarges the GUI plane. Keep Z at unit scale because
                    // RenderItem adds its own GUI z offset and zLevel internally; scaling Z
                    // sends that offset outside the GUI projection at large zoom sizes.
                    GlStateManager.scale(scale, scale, 1.0F);
                    GlStateManager.translate(-8.0F, -8.0F, 0.0F);
                    // Match Item Zoom's relative z lift and preserve whatever the caller had
                    // already established for this GUI pass.
                    renderer.zLevel = oldZ + 100.0F;
                    TooltipPreviewRenderers.enableZoomItemLighting(scale);
                    renderer.renderItemAndEffectIntoGUI(minecraft.player, stack, 0, 0);
                } finally {
                    GlStateManager.popMatrix();
                    renderer.zLevel = oldZ;
                    RenderHelper.disableStandardItemLighting();
                }

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
                GlStateManager.popMatrix();
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

    private static Bounds availableArea(GuiContainer container, int mouseX, int width, int height,
                                        int gap) {
        int left = container.getGuiLeft();
        int right = left + container.getXSize();
        int top = container.getGuiTop();
        int bottom = top + container.getYSize();
        int leftWidth = Math.max(0, left - gap);
        int rightWidth = Math.max(0, width - right - gap);
        boolean useLeft;
        if ("left".equals(TooltipConfig.zoomOverlaySide)) {
            useLeft = true;
        } else if ("right".equals(TooltipConfig.zoomOverlaySide)) {
            useLeft = false;
        } else if (mouseX < left) {
            useLeft = false;
        } else if (mouseX > right) {
            useLeft = true;
        } else {
            useLeft = leftWidth * 1.1F >= rightWidth;
        }
        if (useLeft && leftWidth > 0) return new Bounds(0, top, leftWidth, bottom - top);
        if (rightWidth > 0) return new Bounds(right + gap, top, rightWidth, bottom - top);
        return new Bounds(0, 0, width, height);
    }

    private static boolean allowed(ItemStack stack) {
        String id = stack.getItem().getRegistryName() == null ? ""
                : stack.getItem().getRegistryName().toString();
        if (matches(TooltipConfig.zoomOverlayBlacklist, id)) return false;
        if (matches(TooltipConfig.zoomOverlayWhitelist, id)) return true;
        if ("all".equals(TooltipConfig.zoomOverlayScope)) return true;
        Object item = stack.getItem();
        return item instanceof ItemTool || item instanceof ItemSword || item instanceof ItemArmor
                || item instanceof ItemBow || item instanceof ItemShield;
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

    private static final class Bounds {
        final int left, top, width, height;
        Bounds(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }
    }
}
