package neofontrender.addons.chat;

import mnm.mods.tabbychat.ChatManager;
import mnm.mods.tabbychat.TabbyChat;
import mnm.mods.tabbychat.api.Channel;
import mnm.mods.tabbychat.api.Chat;
import mnm.mods.tabbychat.gui.ChatTray;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import neofontrender.addons.api.inline.InlineGlyph;
import neofontrender.addons.tooltips.AddonI18n;

import java.util.ArrayList;
import java.util.List;

/** Shared screen-space context menu for vanilla and embedded Tabby chat. */
public final class ChatContextMenu {
    public static final ChatContextMenu INSTANCE = new ChatContextMenu();

    private static final int ROW_HEIGHT = 18;
    private static final int MIN_WIDTH = 96;
    private static final int PADDING = 8;

    private final List<Item> items = new ArrayList<>();
    private int x;
    private int y;
    private int width;
    private boolean open;
    private boolean tabbyTheme;

    private ChatContextMenu() {}

    public void openHistory(String selectedText, int mouseX, int mouseY) {
        if (selectedText == null || selectedText.isEmpty()) return;
        List<Item> next = new ArrayList<>();
        String player = ChatPlayerActions.findPlayer(selectedText);
        if (player != null) {
            next.add(item("private_message", () -> ChatPlayerActions.startPrivateMessage(player)));
            next.add(item("mute_player", () -> ChatPlayerActions.mute(player)));
        }
        next.add(item("copy", ChatKeyBindings.copyDisplayName(),
                () -> ChatCopyController.copyToClipboard(selectedText)));
        next.add(item("copy_plain", () -> ChatCopyController.copyPlainToClipboard(selectedText)));
        next.add(item("copy_formatted", () -> ChatCopyController.copyFormattedToClipboard(selectedText, false)));
        next.add(item("copy_ampersand", () -> ChatCopyController.copyFormattedToClipboard(selectedText, true)));
        open(next, mouseX, mouseY);
    }

    public void openHistoryRow(String text, int mouseX, int mouseY, Runnable delete) {
        if (text == null || text.isEmpty()) return;
        List<Item> next = new ArrayList<>();
        next.add(item("copy", () -> ChatCopyController.copyToClipboard(text)));
        next.add(item("delete", delete));
        open(next, mouseX, mouseY);
    }

    public void openInput(GuiTextField field, int mouseX, int mouseY) {
        if (field == null) return;
        List<Item> next = new ArrayList<>();
        if (hasSelection(field)) {
            next.add(item("cut", ChatKeyBindings.cutDisplayName(), () -> cutInput(field)));
            next.add(item("copy", ChatKeyBindings.copyDisplayName(), () -> copyInput(field)));
        }
        next.add(item("paste", ChatKeyBindings.pasteDisplayName(), () -> pasteInput(field)));
        next.add(item("select_all", ChatKeyBindings.selectAllDisplayName(), () -> selectAllInput(field)));
        open(next, mouseX, mouseY);
    }

    public void openPlayer(String player, int mouseX, int mouseY) {
        if (player == null || player.isEmpty()) return;
        List<Item> next = new ArrayList<>();
        next.add(item("private_message", () -> ChatPlayerActions.startPrivateMessage(player)));
        next.add(item("mention_player", () -> ChatPlayerActions.mention(player)));
        next.add(item("copy_player", () -> ChatPlayerActions.copyName(player)));
        next.add(item("mute_player", () -> ChatPlayerActions.mute(player)));
        open(next, mouseX, mouseY);
    }

    public void openImage(InlineGlyph glyph, int mouseX, int mouseY) {
        if (glyph == null) return;
        List<Item> next = new ArrayList<>();
        next.add(item("copy_image", glyph::copyImageToClipboard));
        open(next, mouseX, mouseY);
    }

    public void openChannel(Channel channel, int mouseX, int mouseY) {
        if (channel == null) return;
        List<Item> next = new ArrayList<>();
        boolean pinned = ChatTabPinPolicy.isPinned(channel);
        next.add(item(pinned ? "unpin" : "pin", () -> {
            ChatTabPinPolicy.setPinned(channel, !pinned);
            ChatTray tray = tray();
            if (tray != null) tray.refreshPins();
        }));
        next.add(item("delete", () -> TabbyChat.getInstance().getChat().removeChannel(channel)));
        next.add(item("settings", () -> channel.openSettings()));
        open(next, mouseX, mouseY);
    }

    private static ChatTray tray() {
        Chat chat = TabbyChat.getInstance().getChat();
        return chat instanceof ChatManager ? ((ChatManager) chat).getChatBox().getTray() : null;
    }

    public boolean click(int mouseX, int mouseY) {
        if (!open || mouseX < x || mouseX >= x + width || mouseY < y
                || mouseY >= y + items.size() * ROW_HEIGHT) return false;
        int index = (mouseY - y) / ROW_HEIGHT;
        Item selected = items.get(index);
        close();
        selected.action.run();
        return true;
    }

    public void draw(int mouseX, int mouseY) {
        if (!open) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        int height = items.size() * ROW_HEIGHT;
        Theme theme = theme(minecraft);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 500.0F);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        if (theme.borderWidth > 0) {
            Gui.drawRect(x - theme.borderWidth, y - theme.borderWidth,
                    x + width + theme.borderWidth, y + height + theme.borderWidth, theme.border);
        }
        Gui.drawRect(x, y, x + width, y + height, theme.background);
        for (int index = 0; index < items.size(); index++) {
            int rowY = y + index * ROW_HEIGHT;
            if (mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                Gui.drawRect(x + 1, rowY + 1, x + width - 1, rowY + ROW_HEIGHT - 1, theme.hover);
            }
            Item item = items.get(index);
            minecraft.fontRenderer.drawStringWithShadow(item.label, x + PADDING, rowY + 5, theme.text);
            if (!item.shortcut.isEmpty()) {
                int shortcutX = x + width - PADDING - minecraft.fontRenderer.getStringWidth(item.shortcut);
                minecraft.fontRenderer.drawStringWithShadow(item.shortcut, shortcutX, rowY + 5, theme.mutedText);
            }
        }
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
        items.clear();
    }

    private void open(List<Item> next, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft);
        int measured = MIN_WIDTH;
        for (Item entry : next) {
            int shortcutWidth = entry.shortcut.isEmpty() ? 0
                    : minecraft.fontRenderer.getStringWidth(entry.shortcut) + 18;
            measured = Math.max(measured, minecraft.fontRenderer.getStringWidth(entry.label)
                    + shortcutWidth + PADDING * 2);
        }
        width = measured;
        int height = next.size() * ROW_HEIGHT;
        x = Math.max(1, Math.min(mouseX + 2, resolution.getScaledWidth() - width - 1));
        y = Math.max(1, Math.min(mouseY + 2, resolution.getScaledHeight() - height - 1));
        items.clear();
        items.addAll(next);
        tabbyTheme = EnhancedChatConfigAccess.tabbedChatEnabled();
        open = !items.isEmpty();
    }

    private static Item item(String key, Runnable action) {
        return item(key, "", action);
    }

    private static Item item(String key, String shortcut, Runnable action) {
        return new Item(AddonI18n.tr("neofontrender_ui_enhancements.gui.chat.context." + key),
                shortcut == null ? "" : shortcut, action);
    }

    public static boolean hasSelection(GuiTextField field) {
        return field.getCursorPosition() != field.getSelectionEnd();
    }

    public static void cutInput(GuiTextField field) {
        if (!hasSelection(field)) return;
        GuiScreen.setClipboardString(selectedText(field));
        field.writeText("");
    }

    public static void copyInput(GuiTextField field) {
        if (hasSelection(field)) GuiScreen.setClipboardString(selectedText(field));
    }

    public static void pasteInput(GuiTextField field) {
        field.writeText(GuiScreen.getClipboardString());
    }

    public static void selectAllInput(GuiTextField field) {
        field.setCursorPositionEnd();
        field.setSelectionPos(0);
    }

    private static String selectedText(GuiTextField field) {
        int start = Math.min(field.getCursorPosition(), field.getSelectionEnd());
        int end = Math.max(field.getCursorPosition(), field.getSelectionEnd());
        String text = field.getText();
        return text.substring(Math.max(0, Math.min(start, text.length())),
                Math.max(0, Math.min(end, text.length())));
    }

    private Theme theme(Minecraft minecraft) {
        float opacity = minecraft.gameSettings.chatOpacity;
        if (tabbyTheme && ChatStyleConfig.enabled) {
            int text = ChatStyleRenderer.color(ChatStyleConfig.text, opacity);
            return new Theme(ChatStyleRenderer.color(ChatStyleConfig.inputBackground, opacity),
                    ChatStyleRenderer.color(ChatStyleConfig.border, opacity),
                    ChatStyleRenderer.color(ChatStyleConfig.hoveredTab, opacity), text,
                    withAlpha(text, 0.65F), Math.max(0, Math.min(4, ChatStyleConfig.borderWidth)));
        }
        if (tabbyTheme) {
            return new Theme(0xF0353B47, 0xFFB7C4D8, 0xD0526175,
                    0xFFFFFFFF, 0xFFAEB6C2, 1);
        }
        float vanillaOpacity = opacity * 0.9F + 0.1F;
        return new Theme(withAlpha(0xE0101010, vanillaOpacity),
                withAlpha(0xB0606060, vanillaOpacity), withAlpha(0xD0505050, vanillaOpacity),
                withAlpha(0xFFFFFFFF, vanillaOpacity), withAlpha(0xFFAAAAAA, vanillaOpacity), 1);
    }

    private static int withAlpha(int color, float multiplier) {
        int alpha = Math.max(0, Math.min(255, Math.round((color >>> 24) * multiplier)));
        return color & 0x00FFFFFF | alpha << 24;
    }

    private static final class Item {
        private final String label;
        private final String shortcut;
        private final Runnable action;

        private Item(String label, String shortcut, Runnable action) {
            this.label = label;
            this.shortcut = shortcut;
            this.action = action;
        }
    }

    private static final class Theme {
        private final int background;
        private final int border;
        private final int hover;
        private final int text;
        private final int mutedText;
        private final int borderWidth;

        private Theme(int background, int border, int hover, int text, int mutedText, int borderWidth) {
            this.background = background;
            this.border = border;
            this.hover = hover;
            this.text = text;
            this.mutedText = mutedText;
            this.borderWidth = borderWidth;
        }
    }
}
