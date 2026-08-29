package neofontrender.addons.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import mnm.mods.tabbychat.core.GuiNewChatTC;
import neofontrender.addons.mixin.InvokerGuiNewChatHistory;
import neofontrender.addons.ui.NfrUiEnhancements;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists chat history into an embedded H2 database. Messages are written
 * synchronously on arrival (no periodic full-file rewrites) and trimmed in
 * batches. A one-time import migrates the pre-H2 JSON file if present.
 */
public enum ChatHistoryManager {
    INSTANCE;

    private static final int TRIM_BATCH = 4096;
    private static final String LEGACY_SUFFIX = ".migrated";

    private ChatHistoryStore store;
    private Path dataBase;
    private Path legacyFile;
    private String activeScope;
    private boolean restoring;
    private boolean pendingScopeSelection;
    private boolean pendingRestore;
    private final Map<String, Integer> pendingTrim = new HashMap<>();
    private final Map<String, String> lastSent = new HashMap<>();
    private int lastTrimLimit = -1;

    public void initialize() {
        Path config = Minecraft.getMinecraft().gameDir.toPath().resolve("config");
        dataBase = config.resolve("neofontrender-ui-chat-history");
        legacyFile = config.resolve("neofontrender-ui-chat-history.json");
        try {
            store = new ChatHistoryStore(dataBase);
            migrateLegacy();
        } catch (Exception exception) {
            NfrUiEnhancements.LOGGER.warn("Could not open chat history database; history persistence disabled", exception);
            store = null;
        }
    }

    public void recordReceived(ITextComponent component, int id) {
        if (restoring || store == null || !persistenceEnabled() || !EnhancedChatConfig.persistReceived
                || component == null || !ensureActiveScope()) return;
        try {
            store.deleteReceivedById(activeScope, id);
            ChatMessageMetadata metadata = ChatMessageMetadataRegistry.get(component);
            if (metadata == null) {
                metadata = new ChatMessageMetadata(System.currentTimeMillis(), ChatSource.SERVER, "", null);
            }
            store.insertReceived(activeScope, id, metadata.timestamp, metadata,
                    ITextComponent.Serializer.componentToJson(component));
            scheduleTrim();
        } catch (ChatHistoryException exception) {
            NfrUiEnhancements.LOGGER.warn("Could not record chat message", exception);
        }
    }

    public void recordSent(String message) {
        if (restoring || store == null || !persistenceEnabled() || !EnhancedChatConfig.persistSent
                || message == null || !ensureActiveScope()) return;
        try {
            String last = lastSent.get(activeScope);
            if (last == null) {
                List<String> sent = store.loadSent(activeScope);
                last = sent.isEmpty() ? null : sent.get(sent.size() - 1);
                lastSent.put(activeScope, last == null ? "" : last);
            }
            if (message.equals(last)) return;
            store.insertSent(activeScope, message);
            lastSent.put(activeScope, message);
            scheduleTrim();
        } catch (ChatHistoryException exception) {
            NfrUiEnhancements.LOGGER.warn("Could not record sent message", exception);
        }
    }

    public void configChanged() {
        if (store == null) return;
        int limit = EnhancedChatConfig.maxMessages;
        try {
            for (String scope : store.scopes()) {
                store.trimReceived(scope, limit);
                store.trimSent(scope, limit);
            }
        } catch (ChatHistoryException exception) {
            NfrUiEnhancements.LOGGER.warn("Could not trim chat history", exception);
        }
        pendingTrim.clear();
        lastTrimLimit = limit;
    }

    public void scheduleRestore() {
        pendingRestore = persistenceEnabled() && ensureActiveScope();
    }

    @SubscribeEvent
    public void connected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        deactivateScope();
        pendingScopeSelection = persistenceEnabled();
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        deactivateScope();
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (pendingScopeSelection && mc.world != null && ensureActiveScope()) {
            pendingScopeSelection = false;
            pendingRestore = true;
        }
        if (pendingRestore && mc.world != null && mc.ingameGUI != null) restore(mc.ingameGUI.getChatGUI());
    }

    private void restore(GuiNewChat chat) {
        pendingRestore = false;
        if (store == null || activeScope == null) return;
        restoring = true;
        try {
            if (EnhancedChatConfig.persistReceived) {
                chat.clearChatMessages(false);
                for (ChatHistoryStore.ReceivedMessage message : store.loadReceived(activeScope)) {
                    try {
                        ITextComponent component = ITextComponent.Serializer.jsonToComponent(message.json);
                        if (component != null) {
                            ChatMessageMetadataRegistry.put(component, message.metadata);
                            restoreReceived(chat, component, message.msgId);
                        }
                    } catch (RuntimeException exception) {
                        NfrUiEnhancements.LOGGER.warn("Skipping an invalid persisted chat component", exception);
                    }
                }
                chat.refreshChat();
            }
            if (EnhancedChatConfig.persistSent) {
                chat.getSentMessages().clear();
                for (String message : store.loadSent(activeScope)) chat.addToSentMessages(message);
            }
        } catch (ChatHistoryException exception) {
            NfrUiEnhancements.LOGGER.warn("Could not restore chat history", exception);
        } finally {
            restoring = false;
        }
    }

    private static void restoreReceived(GuiNewChat chat, ITextComponent component, int id) {
        if (chat instanceof GuiNewChatTC) {
            ((GuiNewChatTC) chat).nfrUi$restoreMessage(component, id,
                    EnhancedChatConfigAccess.logRestoredHistory());
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int updateCounter = mc.ingameGUI == null ? 0 : mc.ingameGUI.getUpdateCounter();
        ((InvokerGuiNewChatHistory) chat).nfrUi$restoreChatLine(component, id, updateCounter, false);
        if (EnhancedChatConfigAccess.logRestoredHistory()) {
            NfrUiEnhancements.LOGGER.info("[CHAT] {}", sanitizeForLog(component));
        }
    }

    private static String sanitizeForLog(ITextComponent component) {
        return component.getUnformattedText().replace("\r", "\\r").replace("\n", "\\n");
    }

    /** One-time import of the pre-H2 JSON file, renamed away after a successful copy. */
    private void migrateLegacy() throws IOException {
        if (store == null || !Files.isRegularFile(legacyFile) || !store.isEmpty()) return;
        final int[] imported = {0};
        try (BufferedReader reader = Files.newBufferedReader(legacyFile, StandardCharsets.UTF_8)) {
            JsonElement rootElement = new JsonParser().parse(reader);
            if (!rootElement.isJsonObject()) return;
            JsonObject root = rootElement.getAsJsonObject();
            if (!root.has("scopes") || !root.get("scopes").isJsonObject()) return;
            store.runInTransaction(() -> {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("scopes").entrySet()) {
                    String scope = entry.getKey();
                    if (!ChatHistoryScope.valid(scope) || !entry.getValue().isJsonObject()) continue;
                    JsonObject bucket = entry.getValue().getAsJsonObject();
                    JsonArray received = bucket.has("received") && bucket.get("received").isJsonArray()
                            ? bucket.getAsJsonArray("received") : new JsonArray();
                    for (JsonElement element : received) {
                        if (!element.isJsonObject()) continue;
                        JsonObject message = element.getAsJsonObject();
                        if (!message.has("text") || !message.get("text").isJsonPrimitive()) continue;
                        ChatMessageMetadata metadata = readLegacyMetadata(message);
                        store.insertReceived(scope, message.has("id") ? message.get("id").getAsInt() : 0,
                                metadata.timestamp, metadata, message.get("text").getAsString());
                        imported[0]++;
                    }
                    JsonArray sent = bucket.has("sent") && bucket.get("sent").isJsonArray()
                            ? bucket.getAsJsonArray("sent") : new JsonArray();
                    for (JsonElement element : sent) {
                        if (element.isJsonPrimitive()) store.insertSent(scope, element.getAsString());
                    }
                    store.trimReceived(scope, EnhancedChatConfig.maxMessages);
                    store.trimSent(scope, EnhancedChatConfig.maxMessages);
                }
            });
            // The JSON is preserved as a backup under .migrated in case of manual rollback.
            Files.move(legacyFile, legacyFile.resolveSibling(legacyFile.getFileName() + LEGACY_SUFFIX),
                    StandardCopyOption.REPLACE_EXISTING);
            NfrUiEnhancements.LOGGER.info("Migrated {} chat messages from legacy JSON to H2 database", imported[0]);
        } catch (Exception exception) {
            // Rolled back: the store is still empty, so the import retries on next launch.
            NfrUiEnhancements.LOGGER.warn("Could not migrate legacy chat history from {}", legacyFile, exception);
        }
    }

    private static ChatMessageMetadata readLegacyMetadata(JsonObject message) {
        if (!message.has("timestamp")) {
            return new ChatMessageMetadata(System.currentTimeMillis(), ChatSource.SERVER, "", null);
        }
        long timestamp = message.get("timestamp").getAsLong();
        ChatSource source = ChatSource.SERVER;
        if (message.has("source")) {
            try {
                source = ChatSource.valueOf(message.get("source").getAsString());
            } catch (IllegalArgumentException ignored) {}
        }
        String player = message.has("player") ? message.get("player").getAsString() : "";
        java.util.UUID playerId = null;
        if (message.has("playerId")) {
            try {
                playerId = java.util.UUID.fromString(message.get("playerId").getAsString());
            } catch (IllegalArgumentException ignored) {}
        }
        String privatePeer = message.has("privatePeer")
                ? message.get("privatePeer").getAsString() : "";
        boolean outgoing = message.has("outgoing") && message.get("outgoing").getAsBoolean();
        String privateBody = message.has("privateBody")
                ? message.get("privateBody").getAsString() : "";
        return new ChatMessageMetadata(timestamp, source, player, playerId, privatePeer, outgoing, privateBody);
    }

    public ChatHistoryStore store() {
        return store;
    }

    private void scheduleTrim() {
        int limit = EnhancedChatConfig.maxMessages;
        if (limit != lastTrimLimit) {
            pendingTrim.clear();
            lastTrimLimit = limit;
        }
        if (pendingTrim.merge(activeScope, 1, Integer::sum) < TRIM_BATCH) return;
        pendingTrim.remove(activeScope);
        try {
            store.trimReceived(activeScope, limit);
            store.trimSent(activeScope, limit);
        } catch (ChatHistoryException exception) {
            NfrUiEnhancements.LOGGER.warn("Could not trim chat history", exception);
        }
    }

    private boolean ensureActiveScope() {
        String scope = currentScope(Minecraft.getMinecraft());
        if (scope == null) return false;
        if (scope.equals(activeScope)) return true;
        activeScope = scope;
        lastSent.remove(activeScope);
        return true;
    }

    private void deactivateScope() {
        activeScope = null;
        pendingScopeSelection = false;
        pendingRestore = false;
    }

    private static String currentScope(Minecraft mc) {
        if (mc.isSingleplayer()) {
            IntegratedServer server = mc.getIntegratedServer();
            return server == null ? null : ChatHistoryScope.singleplayer(server.getFolderName());
        }
        ServerData server = mc.getCurrentServerData();
        return server == null ? null : ChatHistoryScope.server(server.serverIP);
    }

    private static boolean persistenceEnabled() {
        return EnhancedChatConfigAccess.persistenceEnabled();
    }
}
