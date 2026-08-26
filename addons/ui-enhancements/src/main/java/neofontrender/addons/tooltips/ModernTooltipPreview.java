package neofontrender.addons.tooltips;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Large live preview for the selected tooltip profile. */
final class ModernTooltipPreview extends Widget<ModernTooltipPreview> {
    private final Supplier<String> profileId;
    private final Supplier<String> previewItemId;

    ModernTooltipPreview(Supplier<String> profileId, Supplier<String> previewItemId) {
        this.profileId = profileId;
        this.previewItemId = previewItemId;
    }

    int preferredHeight() {
        return 224;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        super.draw(context, theme);
        int width = getArea().w();
        int height = getArea().h();
        int right = Math.max(4, width - 4);
        int stageTop = 30;
        int stageBottom = Math.max(stageTop, height - 25);
        int middle = Math.max(4, width / 2);
        Gui.drawRect(4, 4, right, stageTop, 0xFF17222E);
        Gui.drawRect(4, stageTop, middle, stageBottom, 0xFF263440);
        Gui.drawRect(middle, stageTop, right, stageBottom, 0xFF566575);
        Gui.drawRect(4, stageBottom, right, Math.max(stageBottom, height - 4), 0xFF17222E);
        Gui.drawRect(4, 4, right, 5, 0xFF00AEB8);
        Gui.drawRect(middle, stageTop, middle + 1, stageBottom, 0x668FA5BA);
        String selected = TooltipConfig.normalizeProfile(profileId.get());

        Platform.setupDrawFont();
        Minecraft minecraft = Minecraft.getMinecraft();
        FontRenderer font = minecraft.fontRenderer;
        font.drawString(AddonI18n.tr("neofontrender_ui_enhancements.gui.preview.tooltip")
                + " · " + AddonI18n.tr("neofontrender_ui_enhancements.gui.profile." + selected),
                10, 8, 0xFFB8C8D8);
        TooltipConfig.Profile profile = TooltipConfig.profile(selected);
        boolean textProfile = isTextProfile(selected);
        float profileScale = textProfile ? profile.textScale : 1.0F;
        float profileOffsetX = textProfile ? profile.offsetX : 0.0F;
        float profileOffsetY = textProfile ? profile.offsetY : 0.0F;
        boolean mapPreview = "quark".equals(selected);
        PreviewData preview = previewData(selected, resolvePreviewStack(previewItemId.get()));
        List<String> lines = preview.lines;
        int contentWidth = measuredWidth(font, preview, profileScale);
        int contentHeight = measuredHeight(font, preview, profileScale);
        int panelWidth = mapPreview ? QuarkMapTooltipLayout.PANEL_SIZE
                : Math.max(140, contentWidth + TooltipConfig.horizontalPadding * 2);
        panelWidth = Math.min(Math.max(1, width - 24), panelWidth);
        int panelHeight = mapPreview ? QuarkMapTooltipLayout.PANEL_SIZE
                : Math.max(36, contentHeight + TooltipConfig.verticalPadding * 2);
        panelHeight = Math.min(Math.max(1, stageBottom - stageTop - 12), panelHeight);
        int panelLeft = Math.max(6, (width - panelWidth) / 2);
        int panelTop = Math.max(stageTop + 6,
                stageTop + (stageBottom - stageTop - panelHeight) / 2);
        int layoutWidth = Math.max(1, panelWidth - TooltipConfig.horizontalPadding * 2);
        int textLeft = panelLeft + TooltipConfig.horizontalPadding;
        int textTop = panelTop + TooltipConfig.verticalPadding;

        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        try {
            ModernTooltipRenderer.drawCompatibleBackground(
                    panelLeft, panelTop, panelWidth, panelHeight, preview.stack);
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            if (mapPreview) drawMapPreview(panelLeft, panelTop);
            ModernTooltipRenderer.drawContent(textLeft, textTop, layoutWidth, lines,
                    preview.compactLines, lines.isEmpty() ? 0 : 1,
                    textProfile ? profile : previewProfile(), font, preview.stack, false);

            if (textProfile) {
                String values = String.format(java.util.Locale.ROOT,
                        AddonI18n.tr("neofontrender_ui_enhancements.gui.preview.tooltip_values"),
                        profile.textScale, profile.offsetX, profile.offsetY);
                font.drawString(values, 10, Math.max(10, height - 14), 0xFF8292A5);
            }
        } finally {
            if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
            if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
            if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
            if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static PreviewData previewData(String id, ItemStack selectedStack) {
        String root = "neofontrender_ui_enhancements.gui.preview.tooltip.";
        if ("thaumcraft".equals(id)) {
            List<String> lines = new ArrayList<>(Arrays.asList(
                    AddonI18n.tr(root + "thaumcraft.title"),
                    AddonI18n.tr(root + "thaumcraft.body"),
                    AddonI18n.tr(root + "thaumcraft.detail")));
            List<Boolean> compact = new ArrayList<>(Arrays.asList(false, true, true));
            appendModName(lines, compact, "Thaumcraft");
            return new PreviewData(lines, compact, ItemStack.EMPTY);
        }
        if ("hei".equals(id)) {
            List<String> lines = new ArrayList<>(Arrays.asList(
                    AddonI18n.tr(root + "hei.title"), AddonI18n.tr(root + "hei.body")));
            List<Boolean> compact = flags(lines.size());
            appendModName(lines, compact, "Just Enough Items");
            return new PreviewData(lines, compact, ItemStack.EMPTY);
        }
        if ("obscure".equals(id)) {
            List<String> lines = new ArrayList<>(Arrays.asList(
                    AddonI18n.tr(root + "obscure.title"),
                    AddonI18n.tr(root + "obscure.body")));
            List<Boolean> compact = flags(lines.size());
            appendModName(lines, compact, "Obscure Tooltips");
            return new PreviewData(lines, compact, ItemStack.EMPTY);
        }
        if ("quark".equals(id)) {
            return new PreviewData(Collections.emptyList(), Collections.emptyList(), ItemStack.EMPTY);
        }
        return vanillaPreview(root, selectedStack);
    }

    private static PreviewData vanillaPreview(String root, ItemStack stack) {
        List<String> lines = new ArrayList<>();
        if (stack != null && !stack.isEmpty()) {
            Minecraft minecraft = Minecraft.getMinecraft();
            try {
                ITooltipFlag flag = minecraft.gameSettings.advancedItemTooltips
                        ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL;
                lines.addAll(stack.getTooltip(minecraft.player, flag));
            } catch (RuntimeException ignored) {
                // A third-party item can require a live world or player while building its tooltip.
            }
            if (lines.isEmpty()) lines.add(stack.getDisplayName());
            appendModName(lines, null, ModNameTooltipHandler.getModName(stack));
            return new PreviewData(lines, flags(lines.size()), stack);
        }
        lines.add(AddonI18n.tr(root + "vanilla.title"));
        appendModName(lines, null, "Minecraft");
        return new PreviewData(lines, flags(lines.size()), ItemStack.EMPTY);
    }

    private static void appendModName(List<String> lines, List<Boolean> compact, String modName) {
        if (!TooltipConfig.modNameEnabled || modName == null || modName.isEmpty()
                || ModNameTooltipSupport.containsModName(lines, modName)) return;
        lines.add(ModNameTooltipSupport.format(TooltipConfig.modNameFormat) + modName);
        if (compact != null) compact.add(false);
    }

    private static List<Boolean> flags(int size) {
        List<Boolean> flags = new ArrayList<>(size);
        for (int i = 0; i < size; i++) flags.add(false);
        return flags;
    }

    private static ItemStack resolvePreviewStack(String value) {
        if (value == null) return ItemStack.EMPTY;
        String id = value.trim();
        int metadata = 0;
        int metadataSeparator = id.lastIndexOf('@');
        if (metadataSeparator > id.indexOf(':')) {
            try {
                metadata = Math.max(0, Integer.parseInt(id.substring(metadataSeparator + 1)));
                id = id.substring(0, metadataSeparator);
            } catch (NumberFormatException ignored) {
                return ItemStack.EMPTY;
            }
        }
        try {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            return item == null ? ItemStack.EMPTY : new ItemStack(item, 1, metadata);
        } catch (RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static void drawMapPreview(int panelLeft, int panelTop) {
        int left = panelLeft + QuarkMapTooltipCompat.PANEL_PADDING;
        int top = panelTop + QuarkMapTooltipCompat.PANEL_PADDING;
        int right = left + QuarkMapTooltipCompat.CONTENT_SIZE;
        int bottom = top + QuarkMapTooltipCompat.CONTENT_SIZE;
        Gui.drawRect(left - 1, top - 1, right + 1, bottom + 1, 0x805E7187);
        Gui.drawRect(left, top, right, bottom, 0xFFB9B58B);
        Gui.drawRect(left + 5, top + 7, left + 30, top + 29, 0xFF58795A);
        Gui.drawRect(left + 33, top + 4, right - 5, top + 36, 0xFF6E91A0);
        Gui.drawRect(left + 9, top + 35, left + 39, bottom - 7, 0xFF8A7658);
        Gui.drawRect(left + 43, top + 41, right - 6, bottom - 8, 0xFF527258);
        Gui.drawRect(left + 30, top + 25, left + 34, top + 29, 0xFFF3E7C2);
    }

    private static int measuredWidth(FontRenderer font, PreviewData preview, float profileScale) {
        int width = 0;
        for (int index = 0; index < preview.lines.size(); index++) {
            float scale = profileScale * (isCompact(font, preview, index) ? 0.5F : 1.0F);
            width = Math.max(width, Math.max(1,
                    Math.round(font.getStringWidth(preview.lines.get(index)) * scale)));
        }
        return width;
    }

    private static int measuredHeight(FontRenderer font, PreviewData preview, float profileScale) {
        int height = 0;
        for (int index = 0; index < preview.lines.size(); index++) {
            int advance = isCompact(font, preview, index)
                    ? ThaumcraftTooltipCompat.COMPACT_LINE_HEIGHT
                    : index == 0 ? Math.max(1, font.FONT_HEIGHT - 1) : TooltipConfig.lineHeight;
            height += Math.max(1, Math.round(advance * profileScale));
        }
        if (preview.lines.size() > 1) height += TooltipConfig.titleGap;
        return height;
    }

    private static boolean isCompact(FontRenderer font, PreviewData preview, int lineIndex) {
        return !font.getUnicodeFlag() && preview.compactLines.get(lineIndex);
    }

    private static boolean isTextProfile(String profileId) {
        return "vanilla".equals(profileId) || "thaumcraft".equals(profileId);
    }

    private static TooltipConfig.Profile previewProfile() {
        TooltipConfig.Profile profile = new TooltipConfig.Profile();
        profile.textScale = 1.0F;
        profile.offsetX = 0.0F;
        profile.offsetY = 0.0F;
        return profile;
    }

    private static final class PreviewData {
        final List<String> lines;
        final List<Boolean> compactLines;
        final ItemStack stack;

        PreviewData(List<String> lines, List<Boolean> compactLines, ItemStack stack) {
            this.lines = lines;
            this.compactLines = compactLines;
            this.stack = stack == null ? ItemStack.EMPTY : stack;
        }
    }

}
