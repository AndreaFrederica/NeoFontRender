package neofontrender.addons.chat;

import neofontrender.addons.vendor.tabbychat.api.Message;
import neofontrender.addons.vendor.tabbychat.core.GuiNewChatTC;
import neofontrender.addons.vendor.tabbychat.gui.ChatArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.mixin.AccessorGuiNewChatFeatures;
import neofontrender.addons.tooltips.AddonI18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum ChatSearchController {
    INSTANCE;

    private static final int HEADER_HEIGHT = 27;
    private static final int FOOTER_HEIGHT = 12;
    private static final int ROW_HEIGHT = 15;
    private static final int MAX_WIDTH = 480;
    private static final int HIGHLIGHT = 0x40FFD54F;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private GuiChat screen;
    private GuiTextField field;
    private final List<ChatSearchEntry> entries = new ArrayList<>();
    private final List<ChatSearchEntry> results = new ArrayList<>();
    private ChatSearchQuery activeQuery = ChatSearchQuery.parse("");
    private String cachedQuery;
    private int cachedEntryCount = -1;
    private int scrollOffset;
    private int selected = -1;
    private long lastClickTime;
    private int lastClickIndex = -1;
    private int x;
    private int y;
    private int width;

    public void open(GuiChat chat) {
        if (!EnhancedChatConfig.enabled || !EnhancedChatConfig.messageSearch || chat == null) return;
        if (screen != chat) close();
        screen = chat;
        updateGeometry();
        if (field == null) {
            field = new GuiTextField(Minecraft.getMinecraft().fontRenderer, x + 7, y + 6,
                    width - 14, 15);
            field.setMaxStringLength(256);
            field.setEnableBackgroundDrawing(false);
        }
        field.setFocused(true);
    }

    public boolean isOpen() {
        return field != null && screen == Minecraft.getMinecraft().currentScreen;
    }

    public boolean handleKeyboard() {
        if (!isOpen() || !Keyboard.getEventKeyState()) return false;
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_ESCAPE) {
            close();
            return true;
        }
        if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) {
            moveSelection(key == Keyboard.KEY_UP ? -1 : 1);
            return true;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            if (selected >= 0 && selected < results.size()) jumpTo(results.get(selected));
            return true;
        }
        field.textboxKeyTyped(Keyboard.getEventCharacter(), key);
        return true;
    }

    @SubscribeEvent
    public void opened(GuiOpenEvent event) {
        if (event.gui != screen) close();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void mouse(MouseEvent event) {
        if (!isOpen() || Minecraft.getMinecraft().currentScreen != screen) return;
        int mouseX = scaledMouseX();
        int mouseY = scaledMouseY();
        int rows = visibleRows();
        int panelHeight = panelHeight(rows);

        int wheel = event.dwheel;
        if (wheel != 0) {
            if (rows > 0 && results.size() > rows) {
                scrollOffset += wheel > 0 ? -3 : 3;
                clampScroll();
            }
            event.setCanceled(true);
            return;
        }
        if (event.button == -1 || !event.buttonstate) return;
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + panelHeight) return;

        if (mouseY < y + HEADER_HEIGHT) {
            if (mouseX >= x + width - 16 && mouseY >= y + 5 && mouseY < y + 21) {
                close();
            } else {
                field.mouseClicked(mouseX, mouseY, 0);
            }
        } else if (mouseY < y + HEADER_HEIGHT + rows * ROW_HEIGHT) {
            int index = scrollOffset + (mouseY - y - HEADER_HEIGHT) / ROW_HEIGHT;
            if (index < 0 || index >= results.size()) return;
            if (event.button == 1) {
                GuiScreen.setClipboardString(results.get(index).text);
                return;
            }
            if (index == lastClickIndex && System.currentTimeMillis() - lastClickTime < 300L) {
                jumpTo(results.get(index));
                return;
            }
            lastClickIndex = index;
            lastClickTime = System.currentTimeMillis();
            selected = index;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void draw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!isOpen() || event.gui != screen) return;
        updateGeometry();
        refreshResults();
        int rows = visibleRows();
        int height = panelHeight(rows);
        int background = background();
        int border = border();

        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 0.0F, 550.0F);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, border);
        Gui.drawRect(x, y, x + width, y + height, background);
        drawHeader();
        drawResults(rows);
        drawFooter(rows, height);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    private void drawHeader() {
        Gui.drawRect(x + 5, y + 4, x + width - 20, y + 23, 0x90202020);
        Gui.drawRect(x + width - 20, y + 4, x + width - 5, y + 23, 0x90202020);
        Minecraft minecraft = Minecraft.getMinecraft();
        if (field.getText().isEmpty()) {
            minecraft.fontRenderer.drawString(AddonI18n.tr(
                    "neofontrender_ui_enhancements.chat.search.placeholder"), x + 9, y + 8, 0x80607080);
        }
        field.drawTextBox();
        String count = results.isEmpty() ? "" : String.format(AddonI18n.tr(
                "neofontrender_ui_enhancements.chat.search.results"), results.size());
        if (!count.isEmpty()) {
            minecraft.fontRenderer.drawString(count, x + width - 15 - minecraft.fontRenderer.getStringWidth(count),
                    y + 8, 0xFF9AA5B1);
        }
        minecraft.fontRenderer.drawString("×", x + width - 16, y + 6, 0xFF9AA5B1);
    }

    private void drawResults(int rows) {
        if (rows == 0) {
            String message = field.getText().trim().isEmpty()
                    ? AddonI18n.tr("neofontrender_ui_enhancements.chat.search.hint")
                    : AddonI18n.tr("neofontrender_ui_enhancements.chat.search.no_results");
            Minecraft.getMinecraft().fontRenderer.drawString(message, x + 8, y + HEADER_HEIGHT + 5, 0x80607080);
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        for (int row = 0; row < rows; row++) {
            int index = scrollOffset + row;
            ChatSearchEntry entry = results.get(index);
            int rowY = y + HEADER_HEIGHT + row * ROW_HEIGHT;
            if (index == selected) Gui.drawRect(x + 2, rowY, x + width - 2, rowY + ROW_HEIGHT, 0x35FFFFFF);
            String prefix = TIME.format(Instant.ofEpochMilli(entry.metadata.timestamp)
                    .atZone(ZoneId.systemDefault())) + " " + sourceLabel(entry.metadata);
            minecraft.fontRenderer.drawString(prefix, x + 6, rowY + 3, 0xFF9AA5B1);
            int textX = x + 10 + minecraft.fontRenderer.getStringWidth(prefix);
            String display = minecraft.fontRenderer.trimStringToWidth(entry.text,
                    Math.max(0, x + width - 6 - textX));
            for (int[] range : activeQuery.highlightRanges(display)) {
                int startX = textX + minecraft.fontRenderer.getStringWidth(display.substring(0, range[0]));
                int endX = textX + minecraft.fontRenderer.getStringWidth(display.substring(0, range[1]));
                Gui.drawRect(startX - 1, rowY + 2, endX + 1, rowY + 13, HIGHLIGHT);
            }
            minecraft.fontRenderer.drawString(display, textX, rowY + 3, 0xFFE6EAF0);
        }
        drawScrollbar(rows);
    }

    private void drawScrollbar(int rows) {
        if (results.size() <= rows) return;
        int listTop = y + HEADER_HEIGHT;
        int track = rows * ROW_HEIGHT;
        int maxOffset = results.size() - rows;
        int thumb = Math.max(12, track * rows / results.size());
        int thumbY = listTop + (track - thumb) * scrollOffset / maxOffset;
        Gui.drawRect(x + width - 3, thumbY, x + width - 1, thumbY + thumb, 0x60FFFFFF);
    }

    private void drawFooter(int rows, int height) {
        Minecraft minecraft = Minecraft.getMinecraft();
        String hint = AddonI18n.tr("neofontrender_ui_enhancements.chat.search.hint_keys");
        minecraft.fontRenderer.drawString(hint, x + 6, y + height - 10, 0x50708090);
    }

    private void refreshResults() {
        String text = field.getText();
        if (text.equals(cachedQuery) && cachedEntryCount == entries.size()) return;
        if (cachedEntryCount != entries.size()) {
            entries.clear();
            entries.addAll(snapshotMessages());
            cachedEntryCount = entries.size();
            scrollOffset = 0;
            selected = -1;
        }
        cachedQuery = text;
        results.clear();
        activeQuery = ChatSearchQuery.parse(text);
        if (text.trim().isEmpty()) return;
        for (ChatSearchEntry entry : entries) {
            if (activeQuery.matches(entry)) results.add(entry);
        }
        clampScroll();
        if (selected >= results.size()) selected = results.size() - 1;
    }

    private static List<ChatSearchEntry> snapshotMessages() {
        Minecraft minecraft = Minecraft.getMinecraft();
        List<ChatSearchEntry> entries = new ArrayList<>();
        if (minecraft.ingameGUI == null) return entries;
        GuiNewChat chat = minecraft.ingameGUI.getChatGUI();
        if (chat instanceof GuiNewChatTC) {
            for (Message message : ((GuiNewChatTC) chat).getChatManager()
                    .getActiveChannel().getMessages()) {
                IChatComponent component = message.getMessage();
                ChatMessageMetadata metadata = message instanceof ChatMessageMetadataCarrier
                        ? ((ChatMessageMetadataCarrier) message).nfrUi$getMessageMetadata() : null;
                long fallback = message.getDate() == null ? System.currentTimeMillis()
                        : message.getDate().getTime();
                entries.add(entry(component, metadata, fallback, message, null));
            }
        } else if (chat instanceof AccessorGuiNewChatFeatures) {
            for (ChatLine line : ((AccessorGuiNewChatFeatures) chat).nfrUi$getDrawnChatLines()) {
                ChatMessageMetadata metadata = line instanceof ChatHeadLineMetadata
                        ? ((ChatHeadLineMetadata) line).nfrUi$getMessageMetadata() : null;
                entries.add(entry(line.func_151461_a(), metadata, System.currentTimeMillis(), null, line));
            }
        }
        Collections.reverse(entries);
        return entries;
    }

    private static ChatSearchEntry entry(IChatComponent component, ChatMessageMetadata metadata, long fallbackTime,
                                         Message tabbyMessage, ChatLine vanillaLine) {
        String text = component == null ? "" : component.getUnformattedText();
        if (ChatMessageMetadataRegistry.isTimestamped(component)) {
            text = text.replaceFirst("^\\[[0-9:APMapm]+]\\s*", "");
        }
        if (metadata == null) {
            String player = ChatHeadResolver.detectName(component);
            if (player == null) player = "";
            ChatSource source = ChatSourceClassifier.classify(false, text, player,
                    EnhancedChatConfig.privateSourcePattern, EnhancedChatConfig.serverSourcePattern,
                    EnhancedChatConfig.playerSourcePattern);
            metadata = new ChatMessageMetadata(fallbackTime, source, player,
                    ChatHeadResolver.detectSender(component));
        }
        return new ChatSearchEntry(text, metadata, tabbyMessage, vanillaLine);
    }

    private void jumpTo(ChatSearchEntry target) {
        close();
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiNewChat chat = minecraft.ingameGUI == null ? null : minecraft.ingameGUI.getChatGUI();
        if (chat instanceof GuiNewChatTC && target.tabbyMessage != null) {
            ChatArea area = ((GuiNewChatTC) chat).getChatManager().getChatBox().getChatArea();
            List<Message> lines = area.getChat();
            int index = -1;
            int counter = target.tabbyMessage.getCounter();
            for (int line = lines.size() - 1; line >= 0; line--) {
                if (lines.get(line).getCounter() == counter) {
                    index = line;
                    break;
                }
            }
            if (index >= 0) {
                int capacity = Math.max(1, area.getVisibleLineCapacity());
                area.setScrollPos(Math.max(0, index - capacity / 2));
            }
        } else if (target.vanillaLine != null) {
            GuiScreen.setClipboardString(target.text);
        }
    }

    private void moveSelection(int delta) {
        if (results.isEmpty()) return;
        selected = Math.max(0, Math.min(results.size() - 1, selected + delta));
        if (selected < scrollOffset) scrollOffset = selected;
        int rows = visibleRows();
        if (rows > 0 && selected >= scrollOffset + rows) scrollOffset = selected - rows + 1;
        clampScroll();
    }

    private int visibleRows() {
        if (field == null || field.getText().trim().isEmpty() || results.isEmpty()) return 0;
        int maxRows = Math.max(3, Math.min(20,
                (new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight).getScaledHeight() - HEADER_HEIGHT - FOOTER_HEIGHT - 40)
                        / ROW_HEIGHT));
        return Math.min(maxRows, results.size() - scrollOffset);
    }

    private int panelHeight(int rows) {
        int listHeight = rows == 0 ? 18 : rows * ROW_HEIGHT;
        return HEADER_HEIGHT + listHeight + FOOTER_HEIGHT;
    }

    private void clampScroll() {
        if (results.isEmpty()) {
            scrollOffset = 0;
            return;
        }
        int maxRows = Math.max(3, Math.min(20,
                (new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight).getScaledHeight() - HEADER_HEIGHT - FOOTER_HEIGHT - 40)
                        / ROW_HEIGHT));
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, results.size() - maxRows)));
    }

    private void updateGeometry() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        width = Math.min(MAX_WIDTH, Math.max(180, resolution.getScaledWidth() - 8));
        x = resolution.getScaledWidth() - width - 4;
        y = 4;
        if (field != null) {
            field.xPosition = x + 7;
            field.yPosition = y + 6;
            field.width = width - 14;
        }
    }

    private void close() {
        field = null;
        screen = null;
        results.clear();
        entries.clear();
        cachedQuery = null;
        cachedEntryCount = -1;
        scrollOffset = 0;
        selected = -1;
        lastClickIndex = -1;
    }

    private static String sourceLabel(ChatMessageMetadata metadata) {
        String player = AddonI18n.tr("neofontrender_ui_enhancements.chat.source.player");
        String server = AddonI18n.tr("neofontrender_ui_enhancements.chat.source.server");
        String privateMessage = AddonI18n.tr("neofontrender_ui_enhancements.chat.source.private");
        String group = AddonI18n.tr("neofontrender_ui_enhancements.chat.source.group");
        switch (metadata.source) {
            case PLAYER: return metadata.playerName.isEmpty() ? "[" + player + "]"
                    : "[" + player + ":" + metadata.playerName + "]";
            case PRIVATE: return metadata.playerName.isEmpty() ? "[" + privateMessage + "]"
                    : "[" + privateMessage + ":" + metadata.playerName + "]";
            case GROUP: return metadata.group.isEmpty() ? "[" + group + "]"
                    : "[" + group + ":" + metadata.group + "]";
            default: return "[" + server + "]";
        }
    }

    private static int background() {
        if (EnhancedChatConfigAccess.tabbedChatEnabled() && ChatStyleConfig.enabled) {
            return ChatStyleRenderer.color(ChatStyleConfig.inputBackground, 1.0F);
        }
        return EnhancedChatConfigAccess.tabbedChatEnabled() ? 0xF0353B47 : 0xE0101010;
    }

    private static int border() {
        if (EnhancedChatConfigAccess.tabbedChatEnabled() && ChatStyleConfig.enabled) {
            return ChatStyleRenderer.color(ChatStyleConfig.border, 1.0F);
        }
        return EnhancedChatConfigAccess.tabbedChatEnabled() ? 0xFFB7C4D8 : 0xB0606060;
    }

    private static int scaledMouseX() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        return Mouse.getX() / resolution.getScaleFactor();
    }

    private static int scaledMouseY() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        return resolution.getScaledHeight() - Mouse.getY() / resolution.getScaleFactor() - 1;
    }
}
