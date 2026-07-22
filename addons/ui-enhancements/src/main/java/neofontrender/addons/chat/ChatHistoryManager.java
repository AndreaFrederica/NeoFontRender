package neofontrender.addons.chat;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import neofontrender.addons.ui.NfrUiEnhancements;

/** Coordinates persistence, connection lifecycle, and live GuiNewChat restoration. */
public enum ChatHistoryManager {
    INSTANCE;

    private static final long SAVE_INTERVAL_MILLIS = 5000L;

    private final ChatHistoryBuffer history = new ChatHistoryBuffer();
    private ChatHistoryStorage storage;
    private boolean restoring;
    private boolean pendingRestore;
    private boolean dirty;
    private long lastSaveMillis;

    public void initialize() {
        storage = new JsonChatHistoryStorageImpl(
                Minecraft.getMinecraft().mcDataDir.toPath()
                        .resolve("config")
                        .resolve("neofontrender-ui-chat-history.json"));
        history.replace(storage.load(), EnhancedChatConfig.maxMessages);
        dirty = false;
    }

    public void recordReceived(IChatComponent component, int id) {
        if (restoring || !persistenceEnabled() || !EnhancedChatConfig.persistReceived || component == null) return;
        try {
            history.recordReceived(
                    id, IChatComponent.Serializer.func_150696_a(component), EnhancedChatConfig.maxMessages);
            dirty = true;
        } catch (RuntimeException exception) {
            NfrUiEnhancements.LOGGER.error("Could not serialize a received chat component", exception);
        }
    }

    public void recordSent(String message) {
        if (restoring || !persistenceEnabled() || !EnhancedChatConfig.persistSent || message == null) return;
        history.recordSent(message, EnhancedChatConfig.maxMessages);
        dirty = true;
    }

    public void configChanged() {
        history.trim(EnhancedChatConfig.maxMessages);
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.ingameGUI != null) {
            GuiNewChat chat = minecraft.ingameGUI.getChatGUI();
            ((ChatHistoryRuntimeAccess) chat).nfrUi$trimHistoryToConfiguredLimit();
        }
        dirty = true;
        saveIfEnabled();
    }

    public void scheduleRestore() {
        pendingRestore = persistenceEnabled();
    }

    @SubscribeEvent
    public void connected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        pendingRestore = persistenceEnabled();
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        saveIfEnabled();
        pendingRestore = false;
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (pendingRestore && minecraft.theWorld != null && minecraft.ingameGUI != null) {
            restore(minecraft.ingameGUI.getChatGUI());
        }
        if (dirty && System.currentTimeMillis() - lastSaveMillis >= SAVE_INTERVAL_MILLIS) saveIfEnabled();
    }

    private void restore(GuiNewChat chat) {
        pendingRestore = false;
        restoring = true;
        try {
            ChatHistoryData snapshot = history.snapshot(
                    EnhancedChatConfig.persistReceived, EnhancedChatConfig.persistSent);
            if (EnhancedChatConfig.persistReceived) {
                chat.clearChatMessages();
                for (ChatHistoryEntry entry : snapshot.received()) {
                    try {
                        IChatComponent component = IChatComponent.Serializer.func_150699_a(entry.json());
                        if (component == null) {
                            NfrUiEnhancements.LOGGER.error("Persisted chat component decoded to null");
                            continue;
                        }
                        chat.printChatMessageWithOptionalDeletion(component, entry.id());
                    } catch (RuntimeException exception) {
                        NfrUiEnhancements.LOGGER.error("Skipping an invalid persisted chat component", exception);
                    }
                }
            }
            if (EnhancedChatConfig.persistSent) {
                chat.getSentMessages().clear();
                for (String message : snapshot.sent()) chat.addToSentMessages(message);
            }
        } finally {
            restoring = false;
        }
    }

    private void saveIfEnabled() {
        if (!persistenceEnabled() || !dirty || storage == null) return;
        lastSaveMillis = System.currentTimeMillis();
        ChatHistoryData snapshot = history.snapshot(
                EnhancedChatConfig.persistReceived, EnhancedChatConfig.persistSent);
        if (storage.save(snapshot)) dirty = false;
    }

    private static boolean persistenceEnabled() {
        return EnhancedChatConfig.enabled && EnhancedChatConfig.persistence;
    }
}
