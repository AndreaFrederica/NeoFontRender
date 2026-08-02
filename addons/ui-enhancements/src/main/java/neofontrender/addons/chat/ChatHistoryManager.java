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
import neofontrender.addons.ui.NfrUiEnhancements;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum ChatHistoryManager {
    INSTANCE;

    private static final int DATA_VERSION = 3;

    private final Map<String, HistoryBucket> histories = new LinkedHashMap<>();
    private Path dataFile;
    private String activeScope;
    private HistoryBucket activeHistory;
    private boolean restoring;
    private boolean pendingScopeSelection;
    private boolean pendingRestore;
    private boolean dirty;
    private long lastSaveMillis;

    public void initialize() {
        dataFile = Minecraft.getMinecraft().gameDir.toPath().resolve("config")
                .resolve("neofontrender-ui-chat-history.json");
        load();
    }

    public void recordReceived(ITextComponent component, int id) {
        if (restoring || !persistenceEnabled() || !EnhancedChatConfig.persistReceived
                || component == null || !ensureActiveScope()) return;
        if (id != 0) {
            for (Iterator<MessageEntry> iterator = activeHistory.received.iterator(); iterator.hasNext();) {
                if (iterator.next().id == id) iterator.remove();
            }
        }
        activeHistory.received.add(new MessageEntry(id, ITextComponent.Serializer.componentToJson(component),
                ChatMessageMetadataRegistry.get(component)));
        trim(activeHistory);
        dirty = true;
    }

    public void recordSent(String message) {
        if (restoring || !persistenceEnabled() || !EnhancedChatConfig.persistSent
                || message == null || !ensureActiveScope()) return;
        List<String> sent = activeHistory.sent;
        if (sent.isEmpty() || !sent.get(sent.size() - 1).equals(message)) sent.add(message);
        trim(activeHistory);
        dirty = true;
    }

    public void configChanged() {
        trimAll();
        if (persistenceEnabled()) {
            ensureActiveScope();
            dirty = true;
            save();
        }
    }

    public void scheduleRestore() {
        pendingRestore = persistenceEnabled() && ensureActiveScope();
    }

    @SubscribeEvent
    public void connected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        save();
        deactivateScope();
        pendingScopeSelection = persistenceEnabled();
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        save();
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
        if (dirty && System.currentTimeMillis() - lastSaveMillis >= 5000L) save();
    }

    private void restore(GuiNewChat chat) {
        pendingRestore = false;
        if (activeHistory == null) return;
        restoring = true;
        try {
            if (EnhancedChatConfig.persistReceived) {
                chat.clearChatMessages(false);
                for (MessageEntry entry : activeHistory.received) {
                    try {
                        ITextComponent component = ITextComponent.Serializer.jsonToComponent(entry.json);
                        if (component != null) {
                            ChatMessageMetadataRegistry.put(component, entry.metadata());
                            chat.printChatMessageWithOptionalDeletion(component, entry.id);
                        }
                    } catch (RuntimeException exception) {
                        NfrUiEnhancements.LOGGER.warn("Skipping an invalid persisted chat component", exception);
                    }
                }
            }
            if (EnhancedChatConfig.persistSent) {
                chat.getSentMessages().clear();
                for (String message : activeHistory.sent) chat.addToSentMessages(message);
            }
        } finally {
            restoring = false;
        }
    }

    private void load() {
        histories.clear();
        if (dataFile == null || !Files.isRegularFile(dataFile)) return;
        try (BufferedReader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            JsonElement rootElement = new JsonParser().parse(reader);
            if (!rootElement.isJsonObject()) return;
            JsonObject root = rootElement.getAsJsonObject();
            if (!root.has("scopes") || !root.get("scopes").isJsonObject()) return;
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("scopes").entrySet()) {
                if (!ChatHistoryScope.valid(entry.getKey()) || !entry.getValue().isJsonObject()) continue;
                HistoryBucket bucket = readBucket(entry.getValue().getAsJsonObject());
                trim(bucket);
                histories.put(entry.getKey(), bucket);
            }
            dirty = false;
        } catch (Exception exception) {
            NfrUiEnhancements.LOGGER.warn("Could not load persisted chat history from {}", dataFile, exception);
        }
    }

    private void save() {
        if (!persistenceEnabled() || !dirty || dataFile == null) return;
        trimAll();
        JsonObject root = new JsonObject();
        root.addProperty("version", DATA_VERSION);
        JsonObject scopes = new JsonObject();
        for (Map.Entry<String, HistoryBucket> entry : histories.entrySet()) {
            scopes.add(entry.getKey(), writeBucket(entry.getValue()));
        }
        root.add("scopes", scopes);

        Path temporary = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(dataFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                writer.write(root.toString());
            }
            try {
                Files.move(temporary, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
            lastSaveMillis = System.currentTimeMillis();
        } catch (IOException exception) {
            NfrUiEnhancements.LOGGER.warn("Could not save chat history to {}", dataFile, exception);
        }
    }

    private static HistoryBucket readBucket(JsonObject object) {
        HistoryBucket bucket = new HistoryBucket();
        JsonArray receivedArray = object.has("received") && object.get("received").isJsonArray()
                ? object.getAsJsonArray("received") : new JsonArray();
        for (JsonElement element : receivedArray) {
            if (!element.isJsonObject()) continue;
            JsonObject message = element.getAsJsonObject();
            if (!message.has("text") || !message.get("text").isJsonPrimitive()) continue;
            bucket.received.add(new MessageEntry(message.has("id") ? message.get("id").getAsInt() : 0,
                    message.get("text").getAsString(), readMetadata(message)));
        }
        JsonArray sentArray = object.has("sent") && object.get("sent").isJsonArray()
                ? object.getAsJsonArray("sent") : new JsonArray();
        for (JsonElement element : sentArray) {
            if (element.isJsonPrimitive()) bucket.sent.add(element.getAsString());
        }
        return bucket;
    }

    private static JsonObject writeBucket(HistoryBucket bucket) {
        JsonObject object = new JsonObject();
        JsonArray receivedArray = new JsonArray();
        if (EnhancedChatConfig.persistReceived) {
            for (MessageEntry entry : bucket.received) {
                JsonObject message = new JsonObject();
                message.addProperty("id", entry.id);
                message.addProperty("text", entry.json);
                if (entry.metadata != null) {
                    message.addProperty("timestamp", entry.metadata.timestamp);
                    message.addProperty("source", entry.metadata.source.name());
                    if (!entry.metadata.playerName.isEmpty()) message.addProperty("player", entry.metadata.playerName);
                    if (entry.metadata.playerId != null) message.addProperty("playerId", entry.metadata.playerId.toString());
                    if (!entry.metadata.privatePeer.isEmpty()) {
                        message.addProperty("privatePeer", entry.metadata.privatePeer);
                    }
                    if (entry.metadata.outgoing) message.addProperty("outgoing", true);
                    if (!entry.metadata.privateBody.isEmpty()) {
                        message.addProperty("privateBody", entry.metadata.privateBody);
                    }
                }
                receivedArray.add(message);
            }
        }
        object.add("received", receivedArray);
        JsonArray sentArray = new JsonArray();
        if (EnhancedChatConfig.persistSent) {
            for (String message : bucket.sent) sentArray.add(message);
        }
        object.add("sent", sentArray);
        return object;
    }

    private boolean ensureActiveScope() {
        String scope = currentScope(Minecraft.getMinecraft());
        if (scope == null) return false;
        if (activeHistory != null && scope.equals(activeScope)) return true;
        activeScope = scope;
        activeHistory = histories.computeIfAbsent(scope, ignored -> new HistoryBucket());
        return true;
    }

    private void deactivateScope() {
        activeScope = null;
        activeHistory = null;
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

    private void trimAll() {
        for (HistoryBucket bucket : histories.values()) trim(bucket);
    }

    private static void trim(HistoryBucket bucket) {
        int limit = EnhancedChatConfig.maxMessages;
        if (bucket.received.size() > limit) {
            bucket.received.subList(0, bucket.received.size() - limit).clear();
        }
        if (bucket.sent.size() > limit) bucket.sent.subList(0, bucket.sent.size() - limit).clear();
    }

    private static boolean persistenceEnabled() {
        return EnhancedChatConfigAccess.persistenceEnabled();
    }

    private static final class HistoryBucket {
        private final List<MessageEntry> received = new ArrayList<>();
        private final List<String> sent = new ArrayList<>();
    }

    private static final class MessageEntry {
        private final int id;
        private final String json;
        private final ChatMessageMetadata metadata;

        private MessageEntry(int id, String json, ChatMessageMetadata metadata) {
            this.id = id;
            this.json = json;
            this.metadata = metadata;
        }

        private ChatMessageMetadata metadata() {
            return metadata == null ? new ChatMessageMetadata(
                    System.currentTimeMillis(), ChatSource.SERVER, "", null) : metadata;
        }
    }

    private static ChatMessageMetadata readMetadata(JsonObject message) {
        if (!message.has("timestamp")) return null;
        long timestamp = message.get("timestamp").getAsLong();
        ChatSource source = ChatSource.SERVER;
        if (message.has("source")) {
            try { source = ChatSource.valueOf(message.get("source").getAsString()); }
            catch (IllegalArgumentException ignored) {}
        }
        String player = message.has("player") ? message.get("player").getAsString() : "";
        java.util.UUID playerId = null;
        if (message.has("playerId")) {
            try { playerId = java.util.UUID.fromString(message.get("playerId").getAsString()); }
            catch (IllegalArgumentException ignored) {}
        }
        String privatePeer = message.has("privatePeer")
                ? message.get("privatePeer").getAsString() : "";
        boolean outgoing = message.has("outgoing") && message.get("outgoing").getAsBoolean();
        String privateBody = message.has("privateBody")
                ? message.get("privateBody").getAsString() : "";
        return new ChatMessageMetadata(timestamp, source, player, playerId, privatePeer,
                outgoing, privateBody);
    }
}
