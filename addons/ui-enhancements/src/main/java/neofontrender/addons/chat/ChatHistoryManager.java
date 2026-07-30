package neofontrender.addons.chat;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.IChatComponent;
import neofontrender.addons.ui.NfrUiEnhancements;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Coordinates per-scope persistence, connection lifecycle, and live GuiNewChat restoration.
 * Histories are isolated by server address or singleplayer world folder; the active scope
 * follows connect/disconnect and world load.
 */
public enum ChatHistoryManager {
    INSTANCE;

    private static final long SAVE_INTERVAL_MILLIS = 5000L;

    private final Map<String, ChatHistoryBuffer> histories = new LinkedHashMap<>();
    private ChatHistoryStorage storage;
    private String activeScope;
    private ChatHistoryBuffer activeHistory;
    private boolean restoring;
    private boolean pendingScopeSelection;
    private boolean pendingRestore;
    private boolean dirty;
    private long lastSaveMillis;

    public void initialize() {
        storage = new JsonChatHistoryStorageImpl(
                Minecraft.getMinecraft().mcDataDir.toPath()
                        .resolve("config")
                        .resolve("neofontrender-ui-chat-history.json"));
        histories.clear();
        for (Map.Entry<String, ChatHistoryData> entry : storage.load().entrySet()) {
            ChatHistoryBuffer buffer = new ChatHistoryBuffer();
            buffer.replace(entry.getValue(), EnhancedChatConfig.maxMessages);
            histories.put(entry.getKey(), buffer);
        }
        activeScope = null;
        activeHistory = null;
        dirty = false;
    }

    public void recordReceived(IChatComponent component, int id) {
        if (restoring || !persistenceEnabled() || !EnhancedChatConfig.persistReceived
                || component == null || !ensureActiveScope()) return;
        try {
            activeHistory.recordReceived(
                    id, IChatComponent.Serializer.func_150696_a(component), EnhancedChatConfig.maxMessages);
            dirty = true;
        } catch (RuntimeException exception) {
            NfrUiEnhancements.LOGGER.error("Could not serialize a received chat component", exception);
        }
    }

    public void recordSent(String message) {
        if (restoring || !persistenceEnabled() || !EnhancedChatConfig.persistSent
                || message == null || !ensureActiveScope()) return;
        activeHistory.recordSent(message, EnhancedChatConfig.maxMessages);
        dirty = true;
    }

    public void configChanged() {
        for (ChatHistoryBuffer buffer : histories.values()) {
            buffer.trim(EnhancedChatConfig.maxMessages);
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.ingameGUI != null) {
            GuiNewChat chat = minecraft.ingameGUI.getChatGUI();
            ((ChatHistoryRuntimeAccess) chat).nfrUi$trimHistoryToConfiguredLimit();
        }
        if (persistenceEnabled()) {
            ensureActiveScope();
            dirty = true;
            saveIfEnabled();
        }
    }

    public void scheduleRestore() {
        pendingRestore = persistenceEnabled() && ensureActiveScope();
    }

    @SubscribeEvent
    public void connected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        saveIfEnabled();
        deactivateScope();
        pendingScopeSelection = persistenceEnabled();
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        saveIfEnabled();
        deactivateScope();
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (pendingScopeSelection && minecraft.theWorld != null && ensureActiveScope()) {
            pendingScopeSelection = false;
            pendingRestore = true;
        }
        if (pendingRestore && minecraft.theWorld != null && minecraft.ingameGUI != null) {
            restore(minecraft.ingameGUI.getChatGUI());
        }
        if (dirty && System.currentTimeMillis() - lastSaveMillis >= SAVE_INTERVAL_MILLIS) saveIfEnabled();
    }

    private void restore(GuiNewChat chat) {
        pendingRestore = false;
        if (activeHistory == null) return;
        restoring = true;
        try {
            ChatHistoryData snapshot = activeHistory.snapshot(
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
        Map<String, ChatHistoryData> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, ChatHistoryBuffer> entry : histories.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().snapshot(
                    EnhancedChatConfig.persistReceived, EnhancedChatConfig.persistSent));
        }
        if (storage.save(snapshot)) dirty = false;
    }

    private boolean ensureActiveScope() {
        String scope = currentScope(Minecraft.getMinecraft());
        if (scope == null) return false;
        if (activeHistory != null && scope.equals(activeScope)) return true;
        activeScope = scope;
        activeHistory = histories.get(scope);
        if (activeHistory == null) {
            activeHistory = new ChatHistoryBuffer();
            histories.put(scope, activeHistory);
        }
        return true;
    }

    private void deactivateScope() {
        activeScope = null;
        activeHistory = null;
        pendingScopeSelection = false;
        pendingRestore = false;
    }

    private static String currentScope(Minecraft minecraft) {
        if (minecraft.isSingleplayer()) {
            IntegratedServer server = minecraft.getIntegratedServer();
            return server == null ? null : ChatHistoryScope.singleplayer(server.getFolderName());
        }
        ServerData server = minecraft.func_147104_D();
        return server == null ? null : ChatHistoryScope.server(server.serverIP);
    }

    private static boolean persistenceEnabled() {
        return EnhancedChatConfigAccess.persistenceEnabled();
    }
}
