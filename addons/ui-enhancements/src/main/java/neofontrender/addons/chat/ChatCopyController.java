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
import net.minecraftforge.client.event.GuiScreenEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
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
        if (!EnhancedChatFeatures.copySelection()) {
            ChatContextMenu.INSTANCE.close();
            dragging = false;
            leftDown = Mouse.isButtonDown(0);
            rightDown = Mouse.isButtonDown(1);
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiNewChat chat = minecraft.ingameGUI.getChatGUI();
        if (!(chat instanceof AccessorGuiNewChatFeatures)) return;

        boolean left = Mouse.isButtonDown(0);
        boolean right = Mouse.isButtonDown(1);
        boolean leftPressed = left && !leftDown;
        boolean leftReleased = !left && leftDown;
        boolean rightPressed = right && !rightDown;
        leftDown = left;
        rightDown = right;
        handleMouse(minecraft, chat, event.mouseX, event.mouseY,
                left, leftPressed, leftReleased, rightPressed);

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
            int x1 = 2 + Math.round(scale * (textOffset
                    + minecraft.fontRenderer.getStringWidth(value.substring(0, range.start))));
            int x2 = 2 + Math.round(scale * (textOffset
                    + minecraft.fontRenderer.getStringWidth(value.substring(0, range.end))));
            int y2 = bottom - Math.round(scale * row * minecraft.fontRenderer.FONT_HEIGHT);
            int y1 = bottom - Math.round(scale * (row + 1) * minecraft.fontRenderer.FONT_HEIGHT);
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
            if (inside(input, mouseX, mouseY)) {
                menu.openInput(input, mouseX, mouseY);
                return;
            }
            if (selection.hasSelection()) {
                Hit hit = hit(chat, false);
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
        return EnhancedChatFeatures.ampersandFormatting() ? value.replace('§', '&') : value;
    }

    static void copyPlainToClipboard(String value) {
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(value);
        if (clean != null && !clean.isEmpty()) GuiScreen.setClipboardString(clean);
    }

    static void copyFormattedToClipboard(String value, boolean ampersand) {
        if (value == null || value.isEmpty()) return;
        GuiScreen.setClipboardString(ampersand ? value.replace('§', '&') : value);
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
        int localY = (int) Math.floor((Mouse.getY() / (float) scaleFactor - 27.0F) / chatScale);
        int visibleCount = Math.min(chat.func_146232_i(), lines(chat).size());
        if (visibleCount == 0 || (!clamp && (panelX < 0
                || panelX > chat.func_146228_f() / chatScale
                || localY < 0 || localY >= visibleCount * minecraft.fontRenderer.FONT_HEIGHT))) {
            return null;
        }
        int row = Math.max(0, Math.min(visibleCount - 1,
                localY / minecraft.fontRenderer.FONT_HEIGHT));
        AccessorGuiNewChatFeatures accessor = (AccessorGuiNewChatFeatures) chat;
        int index = Math.max(0, Math.min(lines(chat).size() - 1, row + accessor.nfrUi$getScrollPos()));
        ChatLine line = lines(chat).get(index);
        String value = text(line);
        int textX = Math.max(0, panelX - ChatHeadRenderer.textOffset());
        int position = minecraft.fontRenderer.trimStringToWidth(value, textX).length();
        return new Hit(line, Math.max(0, Math.min(value.length(), position)));
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

        private Hit(ChatLine line, int position) {
            this.line = line;
            this.position = position;
        }
    }
}
