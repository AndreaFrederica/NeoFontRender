package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/** Shared geometry and drawing for the item title header. */
final class TooltipHeaderLayout {
    static final int ICON_SIZE = 16;
    static final int ICON_SLOT = 20;
    static final int ICON_FRAME_INSET = 1;
    static final int ICON_FRAME_SIZE = ICON_SIZE + ICON_FRAME_INSET * 2;
    static final int RARITY_HEIGHT = 9;
    static final int RARITY_BOTTOM_GAP = 3;
    static final int MIN_HEIGHT = 22;

    private TooltipHeaderLayout() {}

    static boolean hasIcon(ItemStack stack) {
        return TooltipConfig.headerIconEnabled && stack != null && !stack.isEmpty();
    }

    static boolean hasRarity(ItemStack stack) {
        return TooltipConfig.rarityEnabled && stack != null && !stack.isEmpty()
                && stack.getRarity() != null;
    }

    static int titleInset(ItemStack stack) {
        return hasIcon(stack) ? ICON_SLOT : 0;
    }

    /**
     * Returns the text origin inside the title content box. Keeping this calculation here
     * makes the immediate and retained render paths agree for every title alignment mode.
     */
    static int titleTextOffset(ItemStack stack, int contentWidth, int renderedWidth) {
        int inset = titleInset(stack);
        int available = Math.max(1, contentWidth - inset);
        int spare = Math.max(0, available - Math.max(0, renderedWidth));
        String alignment = TooltipConfig.titleAlignment;
        // A few integrations still toggle the pre-0.7 boolean directly; preserve that
        // behavior until they migrate to the explicit alignment value.
        if ("center".equals(alignment) && !TooltipConfig.centerTitle) alignment = "left";
        int alignmentOffset = "right".equals(alignment) ? spare
                : "center".equals(alignment) ? spare / 2 : 0;
        return inset + alignmentOffset;
    }

    static boolean hasIconDecoration() {
        return TooltipConfig.headerIconFrameEnabled
                || TooltipConfig.headerIconBackgroundEnabled;
    }

    static int iconDecorationInset() {
        return hasIconDecoration() ? ICON_FRAME_INSET : 0;
    }

    static int iconDecorationSize() {
        return hasIconDecoration() ? ICON_FRAME_SIZE : ICON_SIZE;
    }

    static int requiredContentWidth(int titleWidth, ItemStack stack, FontRenderer font) {
        int rarityWidth = font == null ? 0 : font.getStringWidth(rarityLabel(stack));
        return Math.max(1, Math.max(titleWidth, rarityWidth) + titleInset(stack));
    }

    static int extraHeight(ItemStack stack, int titleHeight) {
        if (!hasIcon(stack) && !hasRarity(stack)) return 0;
        int headerHeight = titleHeight + (hasRarity(stack)
                ? RARITY_HEIGHT + RARITY_BOTTOM_GAP : 0);
        if (hasIcon(stack)) headerHeight = Math.max(MIN_HEIGHT, headerHeight);
        return Math.max(0, headerHeight - titleHeight);
    }

    static String rarityLabel(ItemStack stack) {
        if (!hasRarity(stack)) return "";
        return AddonI18n.tr(rarityTranslationKey(stack.getRarity()));
    }

    static String rarityTranslationKey(EnumRarity rarity) {
        String suffix;
        if (rarity == EnumRarity.UNCOMMON) suffix = "uncommon";
        else if (rarity == EnumRarity.RARE) suffix = "rare";
        else if (rarity == EnumRarity.EPIC) suffix = "epic";
        else suffix = "common";
        return "neofontrender_ui_enhancements.tooltip.rarity." + suffix;
    }

    static int rarityColor(ItemStack stack) {
        if (!hasRarity(stack)) return TooltipConfig.textColor;
        EnumRarity rarity = stack.getRarity();
        if (rarity == EnumRarity.UNCOMMON) return 0xFFFFFF55;
        if (rarity == EnumRarity.RARE) return 0xFF55FFFF;
        if (rarity == EnumRarity.EPIC) return 0xFFFF55FF;
        return TooltipConfig.textColor;
    }

    static void drawIcon(ItemStack stack, int x, int y) {
        if (!hasIcon(stack)) return;
        RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
        if (renderItem == null) return;
        float appearance = TooltipPreviewRenderers.headerAnimationProgress(stack);
        float iconScale = 0.78F + 0.22F * appearance;
        float oldZLevel = renderItem.zLevel;
        try (ModernTooltipRenderer.CallerGlState ignored =
                     ModernTooltipRenderer.CallerGlState.capture()) {
            GlStateManager.pushMatrix();
            try {
                float centerX = x + ICON_SIZE / 2.0F;
                float centerY = y + ICON_SIZE / 2.0F;
                GlStateManager.translate(centerX, centerY, 0.0F);
                GlStateManager.scale(iconScale, iconScale, 1.0F);
                GlStateManager.translate(-centerX, -centerY, 0.0F);
                drawIconDecoration(stack, x, y, appearance);
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
                TooltipPreviewRenderers.preparePreviewDepthLayer();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                RenderHelper.enableGUIStandardItemLighting();
                renderItem.zLevel = 500.0F;
                renderItem.renderItemAndEffectIntoGUI(stack, x, y);
                renderItem.renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRenderer,
                        stack, x, y, stack.getCount() > 1 ? Integer.toString(stack.getCount()) : "");
            } finally {
                renderItem.zLevel = oldZLevel;
                RenderHelper.disableStandardItemLighting();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.popMatrix();
            }
        }
    }

    private static void drawIconDecoration(ItemStack stack, int x, int y, float appearance) {
        if (!hasIconDecoration()) return;
        int left = x - ICON_FRAME_INSET;
        int top = y - ICON_FRAME_INSET;
        int right = left + ICON_FRAME_SIZE;
        int bottom = top + ICON_FRAME_SIZE;
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        int fill = animatedColor(TooltipConfig.headerIconBackgroundColor, appearance);
        int border = animatedColor(iconFrameColor(stack), appearance);
        float radius = TooltipConfig.headerIconRounded
                ? TooltipConfig.headerIconCornerRadius : 0.0F;
        if (radius > 0.0F) drawRoundedDecoration(left, top, right, bottom, radius, fill, border);
        else drawSquareDecoration(left, top, right, bottom, fill, border);
    }

    private static void drawRoundedDecoration(int left, int top, int right, int bottom,
                                              float radius, int fill, int border) {
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        try {
            if (TooltipConfig.headerIconBackgroundEnabled) {
                ModernTooltipRenderer.drawRoundedFill(left, top, right, bottom, radius,
                        solidColors(fill));
            }
            if (TooltipConfig.headerIconFrameEnabled) {
                ModernTooltipRenderer.drawRoundedBorder(left, top, right, bottom, radius,
                        1.0F, solidColors(border));
            }
        } finally {
            GlStateManager.shadeModel(GL11.GL_FLAT);
            if (cullEnabled) GlStateManager.enableCull();
            else GlStateManager.disableCull();
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
        }
    }

    private static void drawSquareDecoration(int left, int top, int right, int bottom,
                                             int fill, int border) {
        if (TooltipConfig.headerIconBackgroundEnabled) {
            Gui.drawRect(left, top, right, bottom, fill);
        }
        if (TooltipConfig.headerIconFrameEnabled) {
            Gui.drawRect(left, top, right, top + 1, border);
            Gui.drawRect(left, bottom - 1, right, bottom, border);
            Gui.drawRect(left, top, left + 1, bottom, border);
            Gui.drawRect(right - 1, top, right, bottom, border);
        }
    }

    private static int[] solidColors(int color) {
        return new int[]{color, color, color, color};
    }

    private static int iconFrameColor(ItemStack stack) {
        int configured = TooltipConfig.headerIconFrameColor;
        if (!TooltipConfig.headerIconFrameRarityColor) return configured;
        EnumRarity rarity = stack == null || stack.isEmpty() ? null : stack.getRarity();
        int rgb;
        if (rarity == EnumRarity.UNCOMMON) rgb = 0x00FFFF55;
        else if (rarity == EnumRarity.RARE) rgb = 0x0055FFFF;
        else if (rarity == EnumRarity.EPIC) rgb = 0x00FF55FF;
        else rgb = 0x00AAB4C4;
        return (configured & 0xFF000000) | rgb;
    }

    private static int animatedColor(int color, float appearance) {
        int alpha = color >>> 24;
        return withAlpha(color, Math.round(alpha * appearance));
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF)
                | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    static void drawRarity(ItemStack stack, FontRenderer font, int x, int y) {
        String label = rarityLabel(stack);
        if (label.isEmpty() || font == null) return;
        font.drawStringWithShadow(label, x, y, rarityColor(stack));
    }
}
