package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.event.RenderTooltipEvent;

/** Optional visual replacement for Quark's 1.12 Map Tooltip renderer. */
public final class QuarkMapTooltipCompat {
    static final int CONTENT_SIZE = 64;
    static final int PANEL_PADDING = 4;
    private static final float MAP_SCALE = CONTENT_SIZE / 128.0F;

    private QuarkMapTooltipCompat() {}

    public static boolean render(RenderTooltipEvent.PostText event, boolean requireShift) {
        if (!shouldReplace() || event.getStack().isEmpty()
                || !(event.getStack().getItem() instanceof ItemMap)
                || (requireShift && !GuiScreen.isShiftKeyDown())) {
            return false;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        MapData mapData = ((ItemMap) event.getStack().getItem())
                .getMapData(event.getStack(), minecraft.world);
        if (mapData == null) return false;

        ScaledResolution resolution = new ScaledResolution(minecraft);
        QuarkMapTooltipLayout.Placement placement = QuarkMapTooltipLayout.placeForTooltip(
                resolution.getScaledWidth(), resolution.getScaledHeight(),
                event.getX(), event.getY(), event.getWidth(),
                TooltipConfig.horizontalPadding, TooltipConfig.verticalPadding);
        drawPanel(minecraft, event.getStack(), mapData, placement);
        return true;
    }

    private static boolean shouldReplace() {
        return TooltipConfig.quarkModernMapTooltip && Arc3DRuntimeSupport.isAvailable();
    }

    private static void drawPanel(Minecraft minecraft, ItemStack stack, MapData mapData,
                                  QuarkMapTooltipLayout.Placement placement) {
        ModernTooltipRenderer.drawCompatibleBackground(
                placement.x, placement.y,
                QuarkMapTooltipLayout.PANEL_SIZE, QuarkMapTooltipLayout.PANEL_SIZE, stack);

        // drawCompatibleBackground restores normal GUI state. Quark's PostText contract keeps
        // depth and lighting disabled until every tooltip extension has finished rendering.
        restoreTooltipExtensionState();
        int mapX = placement.x + PANEL_PADDING;
        int mapY = placement.y + PANEL_PADDING;
        Gui.drawRect(mapX - 1, mapY - 1, mapX + CONTENT_SIZE + 1, mapY + CONTENT_SIZE + 1,
                0x805E7187);
        Gui.drawRect(mapX, mapY, mapX + CONTENT_SIZE, mapY + CONTENT_SIZE, 0xFF111820);

        GlStateManager.pushMatrix();
        try {
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.translate(mapX, mapY, 400.0F);
            GlStateManager.scale(MAP_SCALE, MAP_SCALE, MAP_SCALE);
            minecraft.entityRenderer.getMapItemRenderer().renderMap(mapData, false);
        } finally {
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
            restoreTooltipExtensionState();
        }
    }

    private static void restoreTooltipExtensionState() {
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
    }

}
