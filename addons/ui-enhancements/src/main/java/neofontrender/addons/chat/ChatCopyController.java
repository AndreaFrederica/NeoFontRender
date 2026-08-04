package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.mixin.AccessorGuiChatFeatures;
import neofontrender.addons.mixin.AccessorGuiNewChatFeatures;
import org.lwjgl.input.Mouse;
import neofontrender.addons.api.inline.InlineTextEngine;
import neofontrender.addons.api.inline.InlineTextLayout;

import java.util.List;
import java.util.Map;

public final class ChatCopyController {
    public static final ChatCopyController INSTANCE = new ChatCopyController();
    public static final int SELECTION_COLOR = 0x605A8DEE;

    private final ChatSelectionModel<ChatLine> selection = new ChatSelectionModel<>();
    private boolean dragging;
    private String selectedHistoryText = "";

    private ChatCopyController() {}

    @SubscribeEvent
    public void onChatOpened(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiChat) {
            selection.clear();
            dragging = false;
            selectedHistoryText = "";
            ChatContextMenu.INSTANCE.close();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseInputBefore(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiChat)) return;
        int button = Mouse.getEventButton();
        if (button < 0 || !Mouse.getEventButtonState()) return;
        int mouseX = mouseX();
        int mouseY = mouseY();

        if (ChatContextMenu.INSTANCE.isOpen()) {
            if (button == 0 && ChatContextMenu.INSTANCE.click(mouseX, mouseY)) {
                event.setCanceled(true);
                return;
            }
            ChatContextMenu.INSTANCE.close();
        }

        if (EnhancedChatConfigAccess.tabbedChatEnabled()) return;
        GuiTextField input = ((AccessorGuiChatFeatures) event.getGui()).nfrUi$getInputField();
        if (button == 1 && EnhancedChatFeatures.copySelection() && inside(input, mouseX, mouseY)) {
            ChatContextMenu.INSTANCE.openInput(input, mouseX, mouseY);
            event.setCanceled(true);
            return;
        }
        GuiNewChat chat = Minecraft.getMinecraft().ingameGUI.getChatGUI();
        ChatInlineImageInteraction.Hit image = ChatInlineImageInteraction.hit(mouseX, mouseY);
        if (button == 1 && image != null) {
            ChatContextMenu.INSTANCE.openImage(image.glyph, mouseX, mouseY);
            event.setCanceled(true);
            return;
        }
        Hit hit = hit(chat, false);
        String player = playerAt(chat, hit);
        if (button == 1 && player != null) {
            ChatContextMenu.INSTANCE.openPlayer(player, mouseX, mouseY);
            event.setCanceled(true);
            return;
        }
        if (button == 0 && hit != null && hit.head && player != null
                && ChatPlayerLinks.activate(player)) {
            event.setCanceled(true);
            return;
        }
        if (button == 1 && EnhancedChatFeatures.copySelection() && selection.hasSelection()) {
            ChatSelectionModel.Range range = hit == null ? null
                    : selection.ranges(lines(chat), ChatCopyController::text).get(hit.line);
            if (range == null || hit.position < range.start || hit.position > range.end) return;
            ChatContextMenu.INSTANCE.openHistory(
                    selection.selectedText(lines(chat), ChatCopyController::text), mouseX, mouseY);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Post event) {
        if (!EnhancedChatFeatures.copySelection() || !(event.getGui() instanceof GuiChat)
                || EnhancedChatConfigAccess.tabbedChatEnabled()) return;
        GuiNewChat chat = Minecraft.getMinecraft().ingameGUI.getChatGUI();
        if (!(chat instanceof AccessorGuiNewChatFeatures)) return;
        int button = Mouse.getEventButton();
        boolean pressed = Mouse.getEventButtonState();
        Hit hit = hit(chat, button != 0 || !pressed && dragging);

        if (button == 0 && pressed) {
            if (hit == null) return;
            ChatContextMenu.INSTANCE.close();
            selectedHistoryText = "";
            selection.begin(hit.line, hit.position);
            dragging = true;
        } else if (dragging && hit != null && (button == -1 || button == 0)) {
            selection.update(hit.line, hit.position);
            if (button == 0 && !pressed) {
                dragging = false;
                selectedHistoryText = selection.selectedText(lines(chat), ChatCopyController::text);
                copyToClipboard(selectedHistoryText);
            }
        } else if (button == 0 && !pressed) {
            dragging = false;
        }
    }

    @SubscribeEvent
    public void onDrawChatScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiChat)) return;
        ChatInlineImageInteraction.draw(event.getMouseX(), event.getMouseY());
        if (!EnhancedChatConfigAccess.tabbedChatEnabled()) {
            GuiNewChat chat = Minecraft.getMinecraft().ingameGUI.getChatGUI();
            Hit hover = hit(chat, false);
            String player = playerAt(chat, hover);
            if (hover != null && hover.head && player != null) {
                ChatPlayerLinks.hoverAvatar(player, event.getMouseX(), event.getMouseY());
            }
        }
        ChatPlayerLinks.drawAvatarTooltip();
        if (!EnhancedChatFeatures.copySelection()) {
            ChatContextMenu.INSTANCE.draw(event.getMouseX(), event.getMouseY());
            return;
        }
        if (EnhancedChatConfigAccess.tabbedChatEnabled() || !selection.hasSelection()) {
            ChatContextMenu.INSTANCE.draw(event.getMouseX(), event.getMouseY());
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiNewChat chat = minecraft.ingameGUI.getChatGUI();
        if (!(chat instanceof AccessorGuiNewChatFeatures)) return;
        List<ChatLine> lines = lines(chat);
        Map<ChatLine, ChatSelectionModel.Range> ranges = selection.ranges(lines, ChatCopyController::text);
        AccessorGuiNewChatFeatures accessor = (AccessorGuiNewChatFeatures) chat;
        int scroll = accessor.nfrUi$getScrollPos();
        float scale = chat.getChatScale();
        float visualOffset = chat instanceof VanillaChatRenderState
                ? ((VanillaChatRenderState) chat).nfrUi$getVisualOffset() : 0.0F;
        int bottom = new ScaledResolution(minecraft).getScaledHeight() - 40 + Math.round(visualOffset);
        int textOffset = ChatHeadRenderer.textOffset();

        for (int row = 0; row + scroll < lines.size() && row < chat.getLineCount(); row++) {
            ChatLine line = lines.get(row + scroll);
            ChatSelectionModel.Range range = ranges.get(line);
            if (range == null || range.start >= range.end) continue;
            String value = text(line);
            InlineTextLayout layout = InlineTextEngine.layout(minecraft.fontRenderer, value);
            int x1 = 2 + Math.round(scale * (textOffset
                    + layout.widthTo(minecraft.fontRenderer, range.start)));
            int x2 = 2 + Math.round(scale * (textOffset
                    + layout.widthTo(minecraft.fontRenderer, range.end)));
            int before = ChatInlineLayout.heightBefore(lines, scroll, row, minecraft.fontRenderer);
            int rowHeight = ChatInlineLayout.lineHeight(line, minecraft.fontRenderer);
            int y2 = bottom - Math.round(scale * before);
            int y1 = bottom - Math.round(scale * (before + rowHeight));
            Gui.drawRect(x1, y1, x2, y2, SELECTION_COLOR);
        }
        ChatContextMenu.INSTANCE.draw(event.getMouseX(), event.getMouseY());
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
            String clean = TextFormatting.getTextWithoutFormattingCodes(value);
            return clean == null ? "" : clean;
        }
        return EnhancedChatFeatures.ampersandFormatting() ? value.replace('\u00A7', '&') : value;
    }

    static void copyPlainToClipboard(String value) {
        String clean = TextFormatting.getTextWithoutFormattingCodes(value);
        if (clean != null && !clean.isEmpty()) GuiScreen.setClipboardString(clean);
    }

    static void copyFormattedToClipboard(String value, boolean ampersand) {
        if (value == null || value.isEmpty()) return;
        GuiScreen.setClipboardString(ampersand ? value.replace('\u00A7', '&') : value);
    }

    private static boolean inside(GuiTextField field, int mouseX, int mouseY) {
        return field != null && mouseX >= field.x && mouseX < field.x + field.width
                && mouseY >= field.y && mouseY < field.y + field.height;
    }

    private static int mouseX() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        return Mouse.getX() / resolution.getScaleFactor();
    }

    private static int mouseY() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        return resolution.getScaledHeight() - Mouse.getY() / resolution.getScaleFactor() - 1;
    }

    private static Hit hit(GuiNewChat chat, boolean clamp) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft);
        int scaleFactor = resolution.getScaleFactor();
        float chatScale = chat.getChatScale();
        int panelX = (int) Math.floor((Mouse.getX() / (float) scaleFactor - 2.0F) / chatScale);
        float visualOffset = chat instanceof VanillaChatRenderState
                ? ((VanillaChatRenderState) chat).nfrUi$getVisualOffset() : 0.0F;
        int localY = (int) Math.floor((Mouse.getY() / (float) scaleFactor - 40.0F
                - visualOffset) / chatScale);
        List<ChatLine> chatLines = lines(chat);
        AccessorGuiNewChatFeatures accessor = (AccessorGuiNewChatFeatures) chat;
        int scroll = accessor.nfrUi$getScrollPos();
        int visibleCount = Math.min(chatLines.size() - Math.min(scroll, chatLines.size()),
                ChatInlineLayout.visibleLineCount(chatLines, scroll, chat.getChatHeight(),
                        minecraft.fontRenderer));
        if (visibleCount == 0 || (!clamp && (panelX < 0 || panelX > chat.getChatWidth() / chatScale
                || localY < 0 || localY >= chat.getChatHeight()))) return null;
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
        int position = InlineTextEngine.layout(minecraft.fontRenderer, value)
                .sourceIndexAt(minecraft.fontRenderer, textX);
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
        return ChatPlayerLinks.playerFrom(chat.getChatComponent(Mouse.getX(), Mouse.getY()));
    }

    private static List<ChatLine> lines(GuiNewChat chat) {
        return ((AccessorGuiNewChatFeatures) chat).nfrUi$getDrawnChatLines();
    }

    private static String text(ChatLine line) {
        return line.getChatComponent().getFormattedText();
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
