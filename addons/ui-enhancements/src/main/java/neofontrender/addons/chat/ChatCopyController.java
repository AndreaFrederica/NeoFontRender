package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.api.inline.InlineTextEngine;
import neofontrender.addons.api.inline.InlineTextLayout;
import neofontrender.addons.cjk.ChatTypographyRenderer;
import neofontrender.addons.mixin.AccessorGuiChatFeatures;
import neofontrender.addons.mixin.AccessorGuiNewChatFeatures;
import org.lwjgl.input.Mouse;

import java.util.List;
import java.util.Map;

/**
 * Chat text selection, clipboard export, and context-menu coordination for vanilla chat.
 * Minecraft 1.7.10 has no mouse-input screen event, so mouse button edges are tracked once
 * per frame from the draw-screen event; vanilla click processing is never canceled.
 */
public final class ChatCopyController {
    public static final ChatCopyController INSTANCE = new ChatCopyController();
    public static final int SELECTION_COLOR = 0x605A8DEE;

    private final ChatSelectionModel<ChatLine> selection = new ChatSelectionModel<>();
    private boolean dragging;
    private boolean leftDown;
    private boolean rightDown;
    private String selectedHistoryText = "";

    private ChatCopyController() {}

    @SubscribeEvent
    public void onChatOpened(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiChat) {
            selection.clear();
            dragging = false;
            leftDown = Mouse.isButtonDown(0);
            rightDown = Mouse.isButtonDown(1);
            selectedHistoryText = "";
            ChatContextMenu.INSTANCE.close();
        }
    }

    @SubscribeEvent
    public void onDrawChatScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiChat)) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiNewChat chat = minecraft.ingameGUI.getChatGUI();
        if (!(chat instanceof AccessorGuiNewChatFeatures)) {
            ChatInlineImageInteraction.draw(event.mouseX, event.mouseY);
            ChatPlayerLinks.drawAvatarTooltip();
            return;
        }

        boolean left = Mouse.isButtonDown(0);
        boolean right = Mouse.isButtonDown(1);
        boolean leftPressed = left && !leftDown;
        boolean leftReleased = !left && leftDown;
        boolean rightPressed = right && !rightDown;
        leftDown = left;
        rightDown = right;
        handleMouse(minecraft, chat, event.mouseX, event.mouseY,
                left, leftPressed, leftReleased, rightPressed);

        ChatInlineImageInteraction.draw(event.mouseX, event.mouseY);
        if (!EnhancedChatConfigAccess.tabbedChatEnabled()) {
            Hit hover = hit(chat, false);
            String player = playerAt(chat, hover);
            if (hover != null && hover.head && player != null) {
                ChatPlayerLinks.hoverAvatar(player, event.mouseX, event.mouseY);
            }
        }
        ChatPlayerLinks.drawAvatarTooltip();

        if (!EnhancedChatFeatures.copySelection()) {
            ChatContextMenu.INSTANCE.draw(event.mouseX, event.mouseY);
            return;
        }
        if (EnhancedChatConfigAccess.tabbedChatEnabled() || !selection.hasSelection()) {
            ChatContextMenu.INSTANCE.draw(event.mouseX, event.mouseY);
            return;
        }

        List<ChatLine> lines = lines(chat);
        Map<ChatLine, ChatSelectionModel.Range> ranges = selection.ranges(lines, ChatCopyController::text);
        AccessorGuiNewChatFeatures accessor = (AccessorGuiNewChatFeatures) chat;
        int scroll = accessor.nfrUi$getScrollPos();
        float scale = chat.func_146244_h();
        float visualOffset = chat instanceof VanillaChatRenderState
                ? ((VanillaChatRenderState) chat).nfrUi$getVisualOffset() : 0.0F;
        ScaledResolution resolution = new ScaledResolution(
                minecraft, minecraft.displayWidth, minecraft.displayHeight);
        // GuiIngame translates the chat box to scaledHeight - 48 and drawChat adds 20 more.
        int bottom = resolution.getScaledHeight() - 28 + Math.round(visualOffset);
        int textOffset = ChatHeadRenderer.textOffset();

        for (int row = 0; row + scroll < lines.size() && row < chat.func_146232_i(); row++) {
            ChatLine line = lines.get(row + scroll);
            ChatSelectionModel.Range range = ranges.get(line);
            if (range == null || range.start >= range.end) continue;
            String value = text(line);
            InlineTextLayout layout = InlineTextEngine.layout(minecraft.fontRenderer, value);
            IChatComponent component = line.func_151461_a();
            boolean positioned = ChatTypographyRenderer.isPositioned(component)
                    && !layout.hasGlyphs();
            float startX = positioned
                    ? ChatTypographyRenderer.xAtFormattedIndex(component, range.start)
                    : layout.widthTo(minecraft.fontRenderer, range.start);
            float endX = positioned
                    ? ChatTypographyRenderer.xAtFormattedIndex(component, range.end)
                    : layout.widthTo(minecraft.fontRenderer, range.end);
            int x1 = 2 + Math.round(scale * (textOffset + startX));
            int x2 = 2 + Math.round(scale * (textOffset + endX));
            int before = ChatInlineLayout.heightBefore(lines, scroll, row, minecraft.fontRenderer);
            int rowHeight = ChatInlineLayout.lineHeight(line, minecraft.fontRenderer);
            int y2 = bottom - Math.round(scale * before);
            int y1 = bottom - Math.round(scale * (before + rowHeight));
            Gui.drawRect(x1, y1, x2, y2, SELECTION_COLOR);
        }
        ChatContextMenu.INSTANCE.draw(event.mouseX, event.mouseY);
    }

    private void handleMouse(Minecraft minecraft, GuiNewChat chat, int mouseX, int mouseY,
                             boolean left, boolean leftPressed, boolean leftReleased,
                             boolean rightPressed) {
        ChatContextMenu menu = ChatContextMenu.INSTANCE;
        if (menu.isOpen() && (leftPressed || rightPressed)) {
            if (leftPressed && menu.click(mouseX, mouseY)) return;
            menu.close();
        }

        if (rightPressed && !EnhancedChatConfigAccess.tabbedChatEnabled()) {
            GuiTextField input = ((AccessorGuiChatFeatures) minecraft.currentScreen).nfrUi$getInputField();
            if (EnhancedChatFeatures.copySelection() && inside(input, mouseX, mouseY)) {
                menu.openInput(input, mouseX, mouseY);
                return;
            }
            ChatInlineImageInteraction.Hit image = ChatInlineImageInteraction.hit(mouseX, mouseY);
            if (image != null) {
                menu.openImage(image.glyph, mouseX, mouseY);
                return;
            }
            Hit hit = hit(chat, false);
            String player = playerAt(chat, hit);
            if (player != null) {
                menu.openPlayer(player, mouseX, mouseY);
                return;
            }
            if (EnhancedChatFeatures.copySelection() && selection.hasSelection()) {
                ChatSelectionModel.Range range = hit == null ? null
                        : selection.ranges(lines(chat), ChatCopyController::text).get(hit.line);
                if (range != null && hit.position >= range.start && hit.position <= range.end) {
                    menu.openHistory(
                            selection.selectedText(lines(chat), ChatCopyController::text), mouseX, mouseY);
                    return;
                }
            }
        }

        if (EnhancedChatConfigAccess.tabbedChatEnabled()) {
            if (leftPressed || leftReleased) dragging = false;
            return;
        }
        if (leftPressed) {
            Hit hit = hit(chat, false);
            String player = hit == null ? null : playerAt(chat, hit);
            if (hit != null && hit.head && player != null && ChatPlayerLinks.activate(player)) return;
            if (hit == null) return;
            menu.close();
            selectedHistoryText = "";
            selection.begin(hit.line, hit.position);
            dragging = true;
        } else if (dragging && left) {
            Hit hit = hit(chat, true);
            if (hit != null) selection.update(hit.line, hit.position);
        } else if (dragging && leftReleased) {
            Hit hit = hit(chat, true);
            if (hit != null) selection.update(hit.line, hit.position);
            dragging = false;
            selectedHistoryText = selection.selectedText(lines(chat), ChatCopyController::text);
            copyToClipboard(selectedHistoryText);
        }
    }

    public static void copyToClipboard(String value) {
        if (value == null || value.isEmpty()) return;
        GuiScreen.setClipboardString(formatForClipboard(value));
    }

    public void setSelectedHistoryText(String value) {
        selectedHistoryText = value == null ? "" : value;
    }

    public boolean copySelectedHistory() {
        if (selectedHistoryText.isEmpty()) return false;
        copyToClipboard(selectedHistoryText);
        return true;
    }

    static String formatForClipboard(String value) {
        if (!EnhancedChatFeatures.copyFormattingCodes()) {
            String clean = EnumChatFormatting.getTextWithoutFormattingCodes(value);
            return clean == null ? "" : clean;
        }
        return EnhancedChatFeatures.ampersandFormatting() ? value.replace('\u00A7', '&') : value;
    }

    static void copyPlainToClipboard(String value) {
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(value);
        if (clean != null && !clean.isEmpty()) GuiScreen.setClipboardString(clean);
    }

    static void copyFormattedToClipboard(String value, boolean ampersand) {
        if (value == null || value.isEmpty()) return;
        GuiScreen.setClipboardString(ampersand ? value.replace('\u00A7', '&') : value);
    }

    private static boolean inside(GuiTextField field, int mouseX, int mouseY) {
        return field != null && mouseX >= field.xPosition && mouseX < field.xPosition + field.width
                && mouseY >= field.yPosition && mouseY < field.yPosition + field.height;
    }

    private static Hit hit(GuiNewChat chat, boolean clamp) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(
                minecraft, minecraft.displayWidth, minecraft.displayHeight);
        int scaleFactor = resolution.getScaleFactor();
        float chatScale = chat.func_146244_h();
        int panelX = (int) Math.floor((Mouse.getX() / (float) scaleFactor - 3.0F) / chatScale);
        float visualOffset = chat instanceof VanillaChatRenderState
                ? ((VanillaChatRenderState) chat).nfrUi$getVisualOffset() : 0.0F;
        int localY = (int) Math.floor((Mouse.getY() / (float) scaleFactor - 27.0F
                - visualOffset) / chatScale);
        List<ChatLine> chatLines = lines(chat);
        AccessorGuiNewChatFeatures accessor = (AccessorGuiNewChatFeatures) chat;
        int scroll = accessor.nfrUi$getScrollPos();
        int visibleCount = Math.min(chatLines.size() - Math.min(scroll, chatLines.size()),
                ChatInlineLayout.visibleLineCount(chatLines, scroll, chat.func_146246_g(),
                        minecraft.fontRenderer));
        if (visibleCount == 0 || (!clamp && (panelX < 0
                || panelX > chat.func_146228_f() / chatScale
                || localY < 0 || localY >= chat.func_146246_g()))) return null;
        int row = 0;
        int before = 0;
        for (; row < visibleCount; row++) {
            int height = ChatInlineLayout.lineHeight(chatLines.get(scroll + row), minecraft.fontRenderer);
            if (localY < before + height) break;
            before += height;
        }
        row = Math.max(0, Math.min(visibleCount - 1, row));
        int index = Math.max(0, Math.min(chatLines.size() - 1, row + scroll));
        ChatLine line = chatLines.get(index);
        String value = text(line);
        int textX = Math.max(0, panelX - ChatHeadRenderer.textOffset());
        IChatComponent component = line.func_151461_a();
        InlineTextLayout layout = InlineTextEngine.layout(minecraft.fontRenderer, value);
        int position = ChatTypographyRenderer.isPositioned(component) && !layout.hasGlyphs()
                ? ChatTypographyRenderer.formattedIndexAt(component, textX)
                : layout.sourceIndexAt(minecraft.fontRenderer, textX);
        boolean head = EnhancedChatFeatures.playerHeads()
                && panelX >= 0 && panelX < ChatHeadRenderer.HEAD_SIZE
                && line instanceof ChatHeadLineMetadata
                && ((ChatHeadLineMetadata) line).nfrUi$isFirstFragment();
        return new Hit(line, Math.max(0, Math.min(value.length(), position)), head);
    }

    private static String playerAt(GuiNewChat chat, Hit hit) {
        if (hit == null) return null;
        if (hit.head && hit.line instanceof ChatHeadLineMetadata) {
            ChatMessageMetadata metadata =
                    ((ChatHeadLineMetadata) hit.line).nfrUi$getMessageMetadata();
            if (metadata != null && !metadata.playerName.isEmpty()) return metadata.playerName;
        }
        return ChatPlayerLinks.playerFrom(chat.func_146236_a(Mouse.getX(), Mouse.getY()));
    }

    private static List<ChatLine> lines(GuiNewChat chat) {
        return ((AccessorGuiNewChatFeatures) chat).nfrUi$getDrawnChatLines();
    }

    private static String text(ChatLine line) {
        return line.func_151461_a().getFormattedText();
    }

    private static final class Hit {
        private final ChatLine line;
        private final int position;
        private final boolean head;

        private Hit(ChatLine line, int position, boolean head) {
            this.line = line;
            this.position = position;
            this.head = head;
        }
    }
}
