package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import neofontrender.addons.api.inline.InlineGlyph;
import neofontrender.addons.api.inline.InlineGlyphHit;
import neofontrender.addons.api.inline.InlineImagePreview;
import neofontrender.addons.api.inline.InlineTextEngine;
import neofontrender.addons.api.inline.InlineTextLayout;
import neofontrender.addons.mixin.AccessorGuiNewChatFeatures;
import neofontrender.addons.tooltips.AddonI18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.awt.Rectangle;
import java.util.List;

/** Hover preview and hit testing for image glyphs in the open vanilla chat. */
public final class ChatInlineImageInteraction {
    private static final int PREVIEW_SIZE = 144;
    private static final int PADDING = 8;
    private static InlineGlyph tabbyHoverGlyph;
    private static Rectangle tabbyHoverBounds;

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
        float scale = chat.func_146244_h();
        float visualOffset = chat instanceof VanillaChatRenderState
                ? ((VanillaChatRenderState) chat).nfrUi$getVisualOffset() : 0.0F;
        int bottom = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight).getScaledHeight() - 40 + Math.round(visualOffset);
        int localX = (int) Math.floor((mouseX - 2.0F) / scale) - ChatHeadRenderer.textOffset();
        int fromBottom = (int) Math.floor((bottom - mouseY) / scale);
        if (localX < 0 || fromBottom < 0) return null;

        int before = 0;
        int maximum = Math.min(lines.size() - scroll,
                ChatInlineLayout.visibleLineCount(lines, scroll, chat.func_146246_g(), minecraft.fontRenderer));
        for (int row = 0; row < maximum; row++) {
            ChatLine line = lines.get(scroll + row);
            int height = ChatInlineLayout.lineHeight(line, minecraft.fontRenderer);
            if (fromBottom >= before && fromBottom < before + height) {
                String text = line.func_151461_a().getFormattedText();
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
        if (!EnhancedChatFeatures.imageGlyphHover() || ChatContextMenu.INSTANCE.isOpen()) return;
        InlineGlyph glyph;
        if (EnhancedChatConfigAccess.tabbedChatEnabled()) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.currentScreen == null) {
                ScaledResolution liveResolution = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
                mouseX = Mouse.getX() * liveResolution.getScaledWidth() / minecraft.displayWidth;
                mouseY = liveResolution.getScaledHeight()
                        - Mouse.getY() * liveResolution.getScaledHeight() / minecraft.displayHeight - 1;
            }
            // Gnetum may replay its cached HUD without asking TabbyChat to lay out the line again.
            // Retain the last glyph's real screen box across those frames and hit-test the live
            // pointer against it. Comparing only with the previous pointer position caused every
            // movement frame to alternate between an empty hit and a freshly published hit.
            if (!insideRetainedBounds(mouseX, mouseY)) {
                tabbyHoverGlyph = null;
                tabbyHoverBounds = null;
                return;
            }
            glyph = published;
        } else {
            Hit hit = hit(mouseX, mouseY);
            glyph = hit == null ? null : hit.glyph;
        }
        if (glyph == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
        boolean natural = Keyboard.isKeyDown(Keyboard.KEY_LMENU)
                || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
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

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 700.0F);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
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
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    private static int withAlpha(int color, float multiplier) {
        int alpha = Math.max(0, Math.min(255, Math.round((color >>> 24) * multiplier)));
        return color & 0x00FFFFFF | alpha << 24;
    }

    /** Receives a Tabby/UIE-local hit and defers its preview until the screen's final overlay pass. */
    public static void publishTabbyHover(@Nullable InlineGlyph glyph,
                                         int glyphX, int glyphY, int glyphWidth, int glyphHeight) {
        tabbyHoverGlyph = glyph;
        tabbyHoverBounds = glyph == null ? null : new Rectangle(glyphX, glyphY,
                Math.max(1, glyphWidth), Math.max(1, glyphHeight));
    }

    /** Clears the retained hit when the chat screen closes. */
    public static void clearTabbyHover() {
        tabbyHoverGlyph = null;
        tabbyHoverBounds = null;
    }

    private static boolean insideRetainedBounds(int mouseX, int mouseY) {
        Rectangle bounds = tabbyHoverBounds;
        return tabbyHoverGlyph != null && bounds != null
                && mouseX >= bounds.x - 1 && mouseX < bounds.x + bounds.width + 1
                && mouseY >= bounds.y - 1 && mouseY < bounds.y + bounds.height + 1;
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
