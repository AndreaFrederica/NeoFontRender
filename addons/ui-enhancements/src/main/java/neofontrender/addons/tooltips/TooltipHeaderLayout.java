package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
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
        return TooltipConfig.headerIconEnabled && stack != null;
    }

    static boolean hasRarity(ItemStack stack) {
        return TooltipConfig.rarityEnabled && stack != null && stack.getRarity() != null;
    }

    static int titleInset(ItemStack stack) {
        return hasIcon(stack) ? ICON_SLOT : 0;
    }

    /**
     * Returns the text origin inside the title content box. Keeping this calculation here
     * makes the immediate and retained render paths agree for every title alignment mode.
     */
    static int titleTextOffset(ItemStack stack, int contentWidth, int renderedWidth) {
        return titleTextOffset(stack, contentWidth, renderedWidth, null);
    }

    /**
     * Calculates a row origin inside one shared text region. Each row uses its own measured
     * width, but the center/right edge is derived from the same available region, so title and
     * rarity remain aligned even when their strings have different widths.
     */
    static int titleTextOffset(ItemStack stack, int contentWidth, int renderedWidth,
                               FontRenderer font) {
        return textOffset(stack, contentWidth, renderedWidth, TooltipConfig.titleAlignment);
    }

    static int titleTextOffset(ItemStack stack, int contentWidth, int renderedWidth,
                               FontRenderer font, float visualLeft) {
        return textOffset(stack, contentWidth, renderedWidth, TooltipConfig.titleAlignment)
                + visualOriginCorrection(visualLeft);
    }

    /** Uses the same final paragraph measurement consumed by the text renderer. */
    static int titleTextOffset(ItemStack stack, int contentWidth, FontRenderer font,
                               String line, boolean compact, float textScale) {
        return titleTextOffset(stack, contentWidth,
                TooltipLayout.measuredLineWidth(font, line, compact, textScale), font);
    }

    static int titleTextOffset(ItemStack stack, int contentWidth, FontRenderer font,
                               String line, boolean compact, float textScale,
                               float visualLeft) {
        return titleTextOffset(stack, contentWidth,
                TooltipLayout.measuredLineWidth(font, line, compact, textScale), font,
                visualLeft);
    }

    static int rarityTextOffset(ItemStack stack, int contentWidth, int renderedWidth,
                                FontRenderer font) {
        return textOffset(stack, contentWidth, renderedWidth, TooltipConfig.rarityAlignment);
    }

    /** Body rows use the same measured width but do not reserve the header icon slot. */
    static int bodyTextOffset(int contentWidth, int renderedWidth) {
        return textOffset(null, contentWidth, renderedWidth, TooltipConfig.bodyAlignment);
    }

    static int bodyTextOffset(int contentWidth, int renderedWidth, float visualLeft) {
        return textOffset(null, contentWidth, renderedWidth, TooltipConfig.bodyAlignment)
                + visualOriginCorrection(visualLeft);
    }

    private static int visualOriginCorrection(float visualLeft) {
        return Float.isFinite(visualLeft) ? Math.round(-visualLeft) : 0;
    }

    private static int textOffset(ItemStack stack, int contentWidth, int renderedWidth,
                                  String alignment) {
        int inset = titleInset(stack);
        int available = Math.max(1, contentWidth - inset);
        int rowWidth = Math.max(0, renderedWidth);
        int spare = Math.max(0, available - rowWidth);
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
        int rarityWidth = font == null || !hasRarity(stack) ? 0
                : (int) Math.ceil(measureRarity(stack, font).bounds.width());
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
        if (rarity == EnumRarity.uncommon) suffix = "uncommon";
        else if (rarity == EnumRarity.rare) suffix = "rare";
        else if (rarity == EnumRarity.epic) suffix = "epic";
        else suffix = "common";
        return "neofontrender_ui_enhancements.tooltip.rarity." + suffix;
    }

    static int rarityColor(ItemStack stack) {
        if (!hasRarity(stack)) return TooltipConfig.textColor;
        EnumRarity rarity = stack.getRarity();
        if (rarity == EnumRarity.uncommon) return 0xFFFFFF55;
        if (rarity == EnumRarity.rare) return 0xFF55FFFF;
        if (rarity == EnumRarity.epic) return 0xFFFF55FF;
        return TooltipConfig.textColor;
    }

    static void drawIcon(ItemStack stack, int x, int y) {
        if (!hasIcon(stack)) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        RenderItem renderItem = RenderItem.getInstance();
        if (renderItem == null) return;
        float appearance = TooltipPreviewRenderers.headerAnimationProgress(stack);
        float iconScale = 0.78F + 0.22F * appearance;
        float oldZLevel = renderItem.zLevel;
        try (ModernTooltipRenderer.CallerGlState ignored =
                     ModernTooltipRenderer.CallerGlState.capture()) {
            GL11.glPushMatrix();
            try {
                float centerX = x + ICON_SIZE / 2.0F;
                float centerY = y + ICON_SIZE / 2.0F;
                GL11.glTranslatef(centerX, centerY, 0.0F);
                GL11.glScalef(iconScale, iconScale, 1.0F);
                GL11.glTranslatef(-centerX, -centerY, 0.0F);
                drawIconDecoration(stack, x, y, appearance);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                TooltipPreviewRenderers.preparePreviewDepthLayer();
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                RenderHelper.enableGUIStandardItemLighting();
                renderItem.zLevel = 500.0F;
                renderItem.renderItemAndEffectIntoGUI(minecraft.fontRenderer,
                        minecraft.getTextureManager(), stack, x, y);
                renderItem.renderItemOverlayIntoGUI(minecraft.fontRenderer,
                        minecraft.getTextureManager(), stack, x, y,
                        stack.stackSize > 1 ? Integer.toString(stack.stackSize) : "");
            } finally {
                renderItem.zLevel = oldZLevel;
                RenderHelper.disableStandardItemLighting();
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glPopMatrix();
            }
        }
    }

    private static void drawIconDecoration(ItemStack stack, int x, int y, float appearance) {
        if (!hasIconDecoration()) return;
        int left = x - ICON_FRAME_INSET;
        int top = y - ICON_FRAME_INSET;
        int right = left + ICON_FRAME_SIZE;
        int bottom = top + ICON_FRAME_SIZE;
        drawDecoration(left, top, right, bottom,
                TooltipConfig.headerIconRounded ? TooltipConfig.headerIconCornerRadius : 0.0F,
                TooltipConfig.headerIconBackgroundEnabled, TooltipConfig.headerIconFrameEnabled,
                animatedColor(TooltipConfig.headerIconBackgroundColor, appearance),
                animatedColor(iconFrameColor(stack), appearance));
    }

    static void drawDecoration(int left, int top, int right, int bottom, float radius,
                               boolean background, boolean frame, int fill, int border) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
        if (radius > 0.0F) drawRoundedDecoration(left, top, right, bottom, radius, fill, border, background, frame);
        else drawSquareDecoration(left, top, right, bottom, fill, border, background, frame);
    }

    private static void drawRoundedDecoration(int left, int top, int right, int bottom,
                                              float radius, int fill, int border, boolean background, boolean frame) {
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        try {
            if (background) {
                ModernTooltipRenderer.drawRoundedFill(left, top, right, bottom, radius,
                        solidColors(fill));
            }
            if (frame) {
                ModernTooltipRenderer.drawRoundedBorder(left, top, right, bottom, radius,
                        1.0F, solidColors(border));
            }
        } finally {
            GL11.glShadeModel(GL11.GL_FLAT);
            if (cullEnabled) GL11.glEnable(GL11.GL_CULL_FACE);
            else GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
    }

    private static void drawSquareDecoration(int left, int top, int right, int bottom,
                                             int fill, int border, boolean background, boolean frame) {
        if (background) {
            Gui.drawRect(left, top, right, bottom, fill);
        }
        if (frame) {
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
        return frameColor(stack, TooltipConfig.headerIconFrameColor, TooltipConfig.headerIconFrameRarityColor);
    }

    static int frameColor(ItemStack stack, int configured, boolean followRarity) {
        if (!followRarity) return configured;
        EnumRarity rarity = stack == null ? null : stack.getRarity();
        int rgb;
        if (rarity == EnumRarity.uncommon) rgb = 0x00FFFF55;
        else if (rarity == EnumRarity.rare) rgb = 0x0055FFFF;
        else if (rarity == EnumRarity.epic) rgb = 0x00FF55FF;
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

    static TooltipTextLine measureRarity(ItemStack stack, FontRenderer font) {
        return TooltipTextLine.measure(font, rarityLabel(stack), rarityColor(stack), true);
    }
}
