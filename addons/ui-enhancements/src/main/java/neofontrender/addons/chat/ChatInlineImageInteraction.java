package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import neofontrender.addons.api.inline.InlineGlyph;
import neofontrender.addons.api.inline.InlineGlyphHit;
import neofontrender.addons.api.inline.InlineImagePreview;
import neofontrender.addons.api.inline.InlineTextEngine;
import neofontrender.addons.api.inline.InlineTextLayout;
import neofontrender.addons.mixin.AccessorGuiNewChatFeatures;
import neofontrender.addons.tooltips.AddonI18n;

import javax.annotation.Nullable;
import java.util.List;

/** Hover preview and hit testing for image glyphs in the open vanilla chat. */
public final class ChatInlineImageInteraction {
    private static final int PREVIEW_SIZE = 144;
    private static final int PADDING = 8;
    private static InlineGlyph tabbyHoverGlyph;
    private static int tabbyHoverX;
    private static int tabbyHoverY;

    private ChatInlineImageInteraction() {}

    @Nullable
    static Hit hit(int mouseX, int mouseY) {
        if (!EnhancedChatFeatures.inlineGlyphs()) return null;
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiNewChat chat = minecraft.ingameGUI.getChatGUI();
        if (!chat.getChatOpen() || EnhancedChatConfigAccess.tabbedChatEnabled()
                || !(chat instanceof AccessorGuiNewChatFeatures)) return null;
        List<ChatLine> lines = ((AccessorGuiNewChatFeatures) chat).nfrUi$getDrawnChatLines();
        int scroll = ((AccessorGuiNewChatFeatures) chat).nfrUi$getScrollPos();
        if (lines.isEmpty() || scroll >= lines.size()) return null;
        float scale = chat.getChatScale();
        float visualOffset = chat instanceof VanillaChatRenderState
                ? ((VanillaChatRenderState) chat).nfrUi$getVisualOffset() : 0.0F;
        int bottom = new ScaledResolution(minecraft).getScaledHeight() - 40 + Math.round(visualOffset);
        int localX = (int) Math.floor((mouseX - 2.0F) / scale) - ChatHeadRenderer.textOffset();
        int fromBottom = (int) Math.floor((bottom - mouseY) / scale);
        if (localX < 0 || fromBottom < 0) return null;

        int before = 0;
        int maximum = Math.min(lines.size() - scroll,
                ChatInlineLayout.visibleLineCount(lines, scroll, chat.getChatHeight(), minecraft.fontRenderer));
        for (int row = 0; row < maximum; row++) {
            ChatLine line = lines.get(scroll + row);
            int height = ChatInlineLayout.lineHeight(line, minecraft.fontRenderer);
            if (fromBottom >= before && fromBottom < before + height) {
                String text = line.getChatComponent().getFormattedText();
                InlineTextLayout layout = InlineTextEngine.layout(minecraft.fontRenderer, text);
                int localY = height - 1 - (fromBottom - before);
                InlineGlyphHit glyph = layout.glyphAt(localX, localY, minecraft.fontRenderer);
                return glyph == null ? null : new Hit(glyph.match().glyph(), glyph.match().start(),
                        glyph.match().end(), text);
            }
            before += height;
        }
        return null;
    }

    static void draw(int mouseX, int mouseY) {
        InlineGlyph published = tabbyHoverGlyph;
        tabbyHoverGlyph = null;
        if (!EnhancedChatFeatures.imageGlyphHover() || ChatContextMenu.INSTANCE.isOpen()) return;
        InlineGlyph glyph;
        if (EnhancedChatConfigAccess.tabbedChatEnabled()) {
            glyph = published;
            mouseX = tabbyHoverX;
            mouseY = tabbyHoverY;
        } else {
            Hit hit = hit(mouseX, mouseY);
            glyph = hit == null ? null : hit.glyph;
        }
        if (glyph == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft);
        boolean natural = GuiScreen.isAltKeyDown();
        int[] imageSize = natural
                ? InlineImagePreview.naturalSize(glyph,
                resolution.getScaledWidth() - PADDING * 4,
                resolution.getScaledHeight() - 46, PREVIEW_SIZE)
                : new int[] { PREVIEW_SIZE, PREVIEW_SIZE };
        String hint = AddonI18n.tr("neofontrender_ui_enhancements.chat.image.copy_hint");
        int descriptionWidth = minecraft.fontRenderer.getStringWidth(glyph.description());
        int maximumContentWidth = resolution.getScaledWidth() - PADDING * 4;
        int contentWidth = Math.max(imageSize[0], Math.min(maximumContentWidth,
                Math.max(descriptionWidth, minecraft.fontRenderer.getStringWidth(hint))));
        int panelWidth = contentWidth + PADDING * 2;
        int panelHeight = imageSize[1] + 30;
        int x = mouseX + 12;
        int y = mouseY + 12;
        if (x + panelWidth > resolution.getScaledWidth() - 2) x = mouseX - panelWidth - 12;
        if (y + panelHeight > resolution.getScaledHeight() - 2) y = mouseY - panelHeight - 12;
        if (x < 2) x = 2;
        if (y < 2) y = 2;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 700);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        int textColor;
        int hintColor;
        if (EnhancedChatConfigAccess.tabbedChatEnabled() && ChatStyleConfig.enabled) {
            float opacity = minecraft.gameSettings.chatOpacity;
            ChatStyleRenderer.panel(panelWidth, panelHeight,
                    ChatStyleConfig.background, ChatStyleConfig.border, opacity);
            textColor = ChatStyleRenderer.color(ChatStyleConfig.text, opacity);
            hintColor = withAlpha(textColor, 0.65F);
        } else {
            Gui.drawRect(-1, -1, panelWidth + 1, panelHeight + 1, 0xD0606872);
            Gui.drawRect(0, 0, panelWidth, panelHeight, 0xF0181C22);
            textColor = 0xFFE5E9EF;
            hintColor = 0xFF9DA8B5;
        }
        int imageX = PADDING + (contentWidth - imageSize[0]) / 2;
        glyph.drawPreview(imageX, PADDING, imageSize[0], imageSize[1], 0xFFFFFFFF);
        String description = minecraft.fontRenderer.trimStringToWidth(glyph.description(),
                panelWidth - PADDING * 2);
        minecraft.fontRenderer.drawStringWithShadow(description, PADDING,
                imageSize[1] + 12, textColor);
        minecraft.fontRenderer.drawStringWithShadow(hint, PADDING,
                imageSize[1] + 22, hintColor);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private static int withAlpha(int color, float multiplier) {
        int alpha = Math.max(0, Math.min(255, Math.round((color >>> 24) * multiplier)));
        return color & 0x00FFFFFF | alpha << 24;
    }

    /** Receives a Tabby/UIE-local hit and defers its preview until the screen's final overlay pass. */
    public static void publishTabbyHover(@Nullable InlineGlyph glyph, int screenX, int screenY) {
        tabbyHoverGlyph = glyph;
        tabbyHoverX = screenX;
        tabbyHoverY = screenY;
    }

    static final class Hit {
        final InlineGlyph glyph;
        final int start;
        final int end;
        final String source;

        private Hit(InlineGlyph glyph, int start, int end, String source) {
            this.glyph = glyph;
            this.start = start;
            this.end = end;
            this.source = source;
        }
    }
}
