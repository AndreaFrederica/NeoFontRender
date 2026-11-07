package neofontrender.addons.tooltips;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
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
import java.awt.image.BufferedImage;
import java.io.InputStream;
import neofontrender.addons.cursor.CursorSvgRasterizer;

/** Large live preview for the selected tooltip profile. */
final class ModernTooltipPreview extends Widget<ModernTooltipPreview> {
    private static final ResourceLocation CURSOR_SVG =
            new ResourceLocation("neofontrender_ui_enhancements", "textures/gui/tooltip_cursor.svg");
    private static final int CURSOR_HOTSPOT_X = 2;
    private static final int CURSOR_HOTSPOT_Y = 2;
    private static final int CURSOR_SIZE = 16;
    private static DynamicTexture cursorTexture;
    private static ResourceLocation cursorTextureLocation;
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
        boolean mapPreview = "quark".equals(selected);
        PreviewData preview = previewData(selected, resolvePreviewStack(previewItemId.get()));
        TooltipConfig.Profile profile = TooltipConfig.profile(selected);
        boolean textProfile = isTextProfile(selected);
        TooltipConfig.Profile activeProfile = textProfile ? profile : previewProfile();
        int maxWidth = Math.max(1, width - 24 - (TooltipConfig.leftPadding + TooltipConfig.rightPadding));
        TooltipLayout layout = mapPreview ? null : TooltipLayout.preview(
                font, preview.stack, preview.lines, preview.compactLines, activeProfile, maxWidth);

        int panelLeft;
        int panelTop;
        int panelWidth;
        int panelHeight;
        int contentX = 0;
        int contentY = 0;
        float stageScale = 1.0F;
        if (mapPreview) {
            panelWidth = QuarkMapTooltipLayout.PANEL_SIZE;
            panelHeight = QuarkMapTooltipLayout.PANEL_SIZE;
            panelLeft = Math.max(6, (width - panelWidth) / 2);
            panelTop = Math.max(stageTop + 6,
                    stageTop + (stageBottom - stageTop - panelHeight) / 2);
        } else {
            TooltipPanelBounds panel = layout.panelBounds();
            panelWidth = Math.max(1, panel.width());
            panelHeight = Math.max(1, panel.height());
            int stageWidth = Math.max(1, right - 4);
            int stageHeight = Math.max(1, stageBottom - stageTop - 12);
            stageScale = Math.min(1.0F,
                    Math.min((float) stageWidth / panelWidth, (float) stageHeight / panelHeight));
            float centerX = width * 0.5F;
            float centerY = stageTop + (stageBottom - stageTop) * 0.5F;
            contentX = Math.round(centerX - panelWidth * stageScale * 0.5F
                    + TooltipConfig.leftPadding * stageScale);
            contentY = Math.round(centerY - panelHeight * stageScale * 0.5F
                    + (TooltipConfig.topPadding - layout.visualTop) * stageScale);
            panelLeft = Math.round(contentX - TooltipConfig.leftPadding * stageScale);
            panelTop = Math.round(contentY + layout.visualTop * stageScale
                    - TooltipConfig.topPadding * stageScale);
            panelWidth = Math.max(1, Math.round(panelWidth * stageScale));
            panelHeight = Math.max(1, Math.round(panelHeight * stageScale));
        }

        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        try {
            if (!mapPreview) {
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.translate(contentX, contentY, 0.0F);
                    GlStateManager.scale(stageScale, stageScale, 1.0F);
                    TooltipPanelBounds panel = layout.panelBounds();
                    ModernTooltipRenderer.drawCompatibleBackground(
                            panel.left, panel.top, panel.width(), panel.height(), preview.stack);
                    GlStateManager.disableLighting();
                    GlStateManager.disableDepth();
                    GlStateManager.enableTexture2D();
                    GlStateManager.enableAlpha();
                    ModernTooltipRenderer.drawContent(layout, font, preview.stack);
                } finally {
                    GlStateManager.popMatrix();
                }
            } else {
                ModernTooltipRenderer.drawCompatibleBackground(
                        panelLeft, panelTop, panelWidth, panelHeight, preview.stack);
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
                drawMapPreview(panelLeft, panelTop);
            }

            if (textProfile) {
                String values = String.format(java.util.Locale.ROOT,
                        AddonI18n.tr("neofontrender_ui_enhancements.gui.preview.tooltip_values"),
                        profile.textScale, profile.offsetX, profile.offsetY);
                font.drawString(values, 10, Math.max(10, height - 14), 0xFF8292A5);
            }
            if (!mapPreview) drawCursorGuide(panelLeft, panelTop, stageScale);
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

    /** Shows the reference cursor used by the live tooltip placement preview. */
    private static void drawCursorGuide(int panelLeft, int panelTop, float scale) {
        int mouseX = Math.round(panelLeft - TooltipConfig.cursorOffsetX * scale);
        int mouseY = Math.round(panelTop - TooltipConfig.cursorOffsetY * scale);
        if (!ensureCursorTexture()) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(cursorTextureLocation);
        int size = Math.max(10, Math.round(CURSOR_SIZE * scale));
        int hotspotX = Math.round(CURSOR_HOTSPOT_X * scale);
        int hotspotY = Math.round(CURSOR_HOTSPOT_Y * scale);
        Gui.drawModalRectWithCustomSizedTexture(mouseX - hotspotX, mouseY - hotspotY,
                0, 0, size, size, 24, 24);
    }

    private static boolean ensureCursorTexture() {
        if (cursorTextureLocation != null) return true;
        try (InputStream input = Minecraft.getMinecraft().getResourceManager()
                .getResource(CURSOR_SVG).getInputStream()) {
            BufferedImage image = CursorSvgRasterizer.rasterize(input);
            cursorTexture = new DynamicTexture(image);
            cursorTextureLocation = Minecraft.getMinecraft().getTextureManager()
                    .getDynamicTextureLocation("nfr_tooltip_cursor", cursorTexture);
            return true;
        } catch (Exception error) {
            return false;
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
