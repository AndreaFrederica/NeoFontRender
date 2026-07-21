package neofontrender.addons.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
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
import java.util.List;

public enum ChatHistoryManager {
    INSTANCE;

    private final List<MessageEntry> received = new ArrayList<>();
    private final List<String> sent = new ArrayList<>();
    private Path dataFile;
    private boolean restoring;
    private boolean pendingRestore;
    private boolean dirty;
    private long lastSaveMillis;

    public void initialize() {
        dataFile = Minecraft.getMinecraft().gameDir.toPath().resolve("config")
                .resolve("neofontrender-ui-chat-history.json");
        load();
    }

    public void recordReceived(ITextComponent component, int id) {
        if (restoring || !persistenceEnabled() || !EnhancedChatConfig.persistReceived || component == null) return;
        if (id != 0) {
            for (Iterator<MessageEntry> iterator = received.iterator(); iterator.hasNext();) {
                if (iterator.next().id == id) iterator.remove();
            }
        }
        received.add(new MessageEntry(id, ITextComponent.Serializer.componentToJson(component)));
        trim();
        dirty = true;
    }

    public void recordSent(String message) {
        if (restoring || !persistenceEnabled() || !EnhancedChatConfig.persistSent || message == null) return;
        if (sent.isEmpty() || !sent.get(sent.size() - 1).equals(message)) sent.add(message);
        trim();
        dirty = true;
    }

    public void configChanged() {
        trim();
        if (persistenceEnabled()) {
            dirty = true;
            save();
        }
    }

    @SubscribeEvent
    public void connected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        pendingRestore = persistenceEnabled();
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        save();
        pendingRestore = false;
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (pendingRestore && mc.world != null && mc.ingameGUI != null) restore(mc.ingameGUI.getChatGUI());
        if (dirty && System.currentTimeMillis() - lastSaveMillis >= 5000L) save();
    }

    private void restore(GuiNewChat chat) {
        pendingRestore = false;
        restoring = true;
        try {
            if (EnhancedChatConfig.persistReceived) {
                chat.clearChatMessages(false);
                for (MessageEntry entry : received) {
                    try {
                        ITextComponent component = ITextComponent.Serializer.jsonToComponent(entry.json);
                        if (component != null) chat.printChatMessageWithOptionalDeletion(component, entry.id);
                    } catch (RuntimeException exception) {
                        NfrUiEnhancements.LOGGER.warn("Skipping an invalid persisted chat component", exception);
                    }
                }
            }
            if (EnhancedChatConfig.persistSent) {
                chat.getSentMessages().clear();
                for (String message : sent) chat.addToSentMessages(message);
            }
        } finally {
            restoring = false;
        }
    }

    private void load() {
        received.clear();
        sent.clear();
        if (!persistenceEnabled() || dataFile == null || !Files.isRegularFile(dataFile)) return;
        try (BufferedReader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            JsonElement rootElement = new JsonParser().parse(reader);
            if (!rootElement.isJsonObject()) return;
            JsonObject root = rootElement.getAsJsonObject();
            JsonArray receivedArray = root.has("received") && root.get("received").isJsonArray()
                    ? root.getAsJsonArray("received") : new JsonArray();
            for (JsonElement element : receivedArray) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                if (!object.has("text")) continue;
                received.add(new MessageEntry(object.has("id") ? object.get("id").getAsInt() : 0,
                        object.get("text").getAsString()));
            }
            JsonArray sentArray = root.has("sent") && root.get("sent").isJsonArray()
                    ? root.getAsJsonArray("sent") : new JsonArray();
            for (JsonElement element : sentArray) if (element.isJsonPrimitive()) sent.add(element.getAsString());
            trim();
            dirty = false;
        } catch (Exception exception) {
            NfrUiEnhancements.LOGGER.warn("Could not load persisted chat history from {}", dataFile, exception);
        }
    }

    private void save() {
        if (!persistenceEnabled() || !dirty || dataFile == null) return;
        trim();
        JsonObject root = new JsonObject();
        JsonArray receivedArray = new JsonArray();
        if (EnhancedChatConfig.persistReceived) {
            for (MessageEntry entry : received) {
                JsonObject object = new JsonObject();
                object.addProperty("id", entry.id);
                object.addProperty("text", entry.json);
                receivedArray.add(object);
            }
        }
        root.add("received", receivedArray);
        JsonArray sentArray = new JsonArray();
        if (EnhancedChatConfig.persistSent) for (String message : sent) sentArray.add(message);
        root.add("sent", sentArray);

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

    private void trim() {
        int limit = EnhancedChatConfig.maxMessages;
        if (received.size() > limit) received.subList(0, received.size() - limit).clear();
        if (sent.size() > limit) sent.subList(0, sent.size() - limit).clear();
    }

    private static boolean persistenceEnabled() {
        return !ExternalChatCompat.tabbyChatLoaded() && EnhancedChatConfig.enabled && EnhancedChatConfig.persistence;
    }

    private static final class MessageEntry {
        private final int id;
        private final String json;

        private MessageEntry(int id, String json) {
            this.id = id;
            this.json = json;
        }
    }
}
