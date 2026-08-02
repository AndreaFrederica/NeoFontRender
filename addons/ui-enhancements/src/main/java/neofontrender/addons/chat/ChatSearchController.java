package neofontrender.addons.chat;

import mnm.mods.tabbychat.api.Message;
import mnm.mods.tabbychat.core.GuiNewChatTC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.mixin.AccessorGuiNewChatFeatures;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import neofontrender.addons.tooltips.AddonI18n;

public enum ChatSearchController {
    INSTANCE;

    private static final int MAX_RESULTS = 12;
    private static final int ROW_HEIGHT = 14;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private GuiChat screen;
    private GuiTextField field;
    private final List<ChatSearchEntry> visible = new ArrayList<>();
    private int x;
    private int y;
    private int width;

    public void open(GuiChat chat) {
        if (!EnhancedChatConfig.enabled || !EnhancedChatConfig.messageSearch || chat == null) return;
        if (screen != chat) close();
        screen = chat;
        updateGeometry();
        if (field == null) {
            field = new GuiTextField(7864, Minecraft.getMinecraft().fontRenderer, x + 7, y + 6,
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
        field.textboxKeyTyped(Keyboard.getEventCharacter(), key);
        return true;
    }

    @SubscribeEvent
    public void opened(GuiOpenEvent event) {
        if (event.getGui() != screen) close();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void mouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!isOpen() || event.getGui() != screen || Mouse.getEventButton() != 0
                || !Mouse.getEventButtonState()) return;
        int mouseX = scaledMouseX();
        int mouseY = scaledMouseY();
        int panelHeight = 27 + visible.size() * ROW_HEIGHT;
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + panelHeight) return;
        if (mouseY < y + 26) {
            field.mouseClicked(mouseX, mouseY, 0);
        } else {
            int index = (mouseY - y - 27) / ROW_HEIGHT;
            if (index >= 0 && index < visible.size()) GuiScreen.setClipboardString(visible.get(index).text);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void draw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!isOpen() || event.getGui() != screen) return;
        updateGeometry();
        refreshResults();
        int height = 27 + visible.size() * ROW_HEIGHT;
        int background = background();
        int border = border();

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 550.0F);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, border);
        Gui.drawRect(x, y, x + width, y + height, background);
        Gui.drawRect(x + 5, y + 4, x + width - 5, y + 23, 0x90202020);
        field.drawTextBox();

        Minecraft minecraft = Minecraft.getMinecraft();
        for (int index = 0; index < visible.size(); index++) {
            ChatSearchEntry entry = visible.get(index);
            int rowY = y + 28 + index * ROW_HEIGHT;
            String prefix = TIME.format(Instant.ofEpochMilli(entry.metadata.timestamp)
                    .atZone(ZoneId.systemDefault())) + " " + sourceLabel(entry.metadata);
            int color = sourceColor(entry.metadata.source);
            minecraft.fontRenderer.drawString(prefix, x + 6, rowY + 2, color);
            int textX = x + 10 + minecraft.fontRenderer.getStringWidth(prefix);
            String value = minecraft.fontRenderer.trimStringToWidth(entry.text,
                    Math.max(0, x + width - 6 - textX));
            minecraft.fontRenderer.drawString(value, textX, rowY + 2, 0xFFE6EAF0);
        }
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private void refreshResults() {
        visible.clear();
        ChatSearchQuery query = ChatSearchQuery.parse(field.getText());
        for (ChatSearchEntry entry : currentMessages()) {
            if (query.matches(entry)) visible.add(entry);
            if (visible.size() >= MAX_RESULTS) break;
        }
    }

    private static List<ChatSearchEntry> currentMessages() {
        Minecraft minecraft = Minecraft.getMinecraft();
        List<ChatSearchEntry> entries = new ArrayList<>();
        if (minecraft.ingameGUI == null) return entries;
        GuiNewChat chat = minecraft.ingameGUI.getChatGUI();
        if (chat instanceof GuiNewChatTC) {
            for (Message message : ((GuiNewChatTC) chat).getChatManager()
                    .getActiveChannel().getMessages()) {
                ITextComponent component = message.getMessage();
                ChatMessageMetadata metadata = message instanceof ChatMessageMetadataCarrier
                        ? ((ChatMessageMetadataCarrier) message).nfrUi$getMessageMetadata() : null;
                long fallback = message.getDate() == null ? System.currentTimeMillis()
                        : message.getDate().getTime();
                entries.add(entry(component, metadata, fallback));
            }
        } else if (chat instanceof AccessorGuiNewChatFeatures) {
            for (ChatLine line : ((AccessorGuiNewChatFeatures) chat).nfrUi$getChatLines()) {
                ChatMessageMetadata metadata = line instanceof ChatHeadLineMetadata
                        ? ((ChatHeadLineMetadata) line).nfrUi$getMessageMetadata() : null;
                entries.add(entry(line.getChatComponent(), metadata, System.currentTimeMillis()));
            }
        }
        return entries;
    }

    private static ChatSearchEntry entry(ITextComponent component, ChatMessageMetadata metadata, long fallbackTime) {
        String text = component == null ? "" : component.getUnformattedText();
        if (ChatMessageMetadataRegistry.isTimestamped(component)) {
            text = text.replaceFirst("^\\[[0-9:APMapm]+]\\s*", "");
        }
        if (metadata == null) {
            UUID sender = ChatHeadResolver.detectSender(component);
            String player = playerName(sender);
            ChatSource source = ChatSourceClassifier.classify(false, text, player,
                    EnhancedChatConfig.privateSourcePattern, EnhancedChatConfig.serverSourcePattern,
                    EnhancedChatConfig.playerSourcePattern);
            metadata = new ChatMessageMetadata(fallbackTime, source, player, sender);
        }
        return new ChatSearchEntry(text, metadata);
    }

    private static String playerName(UUID id) {
        if (id == null || Minecraft.getMinecraft().getConnection() == null) return "";
        return Minecraft.getMinecraft().getConnection().getPlayerInfoMap().stream()
                .filter(info -> id.equals(info.getGameProfile().getId()))
                .map(info -> info.getGameProfile().getName()).findFirst().orElse("");
    }

    private void updateGeometry() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        width = Math.min(380, Math.max(180, resolution.getScaledWidth() - 8));
        x = resolution.getScaledWidth() - width - 4;
        y = 4;
        if (field != null) {
            field.x = x + 7;
            field.y = y + 6;
            field.width = width - 14;
        }
    }

    private void close() {
        field = null;
        screen = null;
        visible.clear();
    }

    private static String sourceLabel(ChatMessageMetadata metadata) {
        String player = AddonI18n.tr("neofontrender_ui_enhancements.chat.source.player");
        String server = AddonI18n.tr("neofontrender_ui_enhancements.chat.source.server");
        String privateMessage = AddonI18n.tr("neofontrender_ui_enhancements.chat.source.private");
        switch (metadata.source) {
            case PLAYER: return metadata.playerName.isEmpty() ? "[" + player + "]"
                    : "[" + player + ":" + metadata.playerName + "]";
            case PRIVATE: return metadata.playerName.isEmpty() ? "[" + privateMessage + "]"
                    : "[" + privateMessage + ":" + metadata.playerName + "]";
            default: return "[" + server + "]";
        }
    }

    private static int sourceColor(ChatSource source) {
        if (source == ChatSource.PLAYER) return 0xFF81C995;
        if (source == ChatSource.PRIVATE) return 0xFFE8A1CF;
        return 0xFF9DB7DF;
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
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        return Mouse.getX() / resolution.getScaleFactor();
    }

    private static int scaledMouseY() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        return resolution.getScaledHeight() - Mouse.getY() / resolution.getScaleFactor() - 1;
    }
}
