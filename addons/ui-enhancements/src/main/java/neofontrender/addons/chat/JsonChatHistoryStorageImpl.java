package neofontrender.addons.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
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
import java.util.List;

/** UTF-8 JSON storage using an atomic sibling-file replacement when supported. */
final class JsonChatHistoryStorageImpl implements ChatHistoryStorage {
    private final Path dataFile;

    JsonChatHistoryStorageImpl(Path dataFile) {
        if (dataFile == null) throw new IllegalArgumentException("dataFile must not be null");
        this.dataFile = dataFile;
    }

    @Override
    public ChatHistoryData load() {
        if (!Files.isRegularFile(dataFile)) return ChatHistoryData.empty();
        try (BufferedReader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            JsonElement element = new JsonParser().parse(reader);
            if (!element.isJsonObject()) throw new JsonParseException("Chat history root must be an object");
            JsonObject root = element.getAsJsonObject();
            JsonArray receivedArray = requiredArray(root, "received");
            JsonArray sentArray = requiredArray(root, "sent");
            List<ChatHistoryEntry> received = new ArrayList<ChatHistoryEntry>(receivedArray.size());
            for (JsonElement receivedElement : receivedArray) {
                if (!receivedElement.isJsonObject()) {
                    throw new JsonParseException("Received history entry must be an object");
                }
                JsonObject object = receivedElement.getAsJsonObject();
                if (!object.has("id") || !object.get("id").isJsonPrimitive()
                        || !object.has("text") || !object.get("text").isJsonPrimitive()) {
                    throw new JsonParseException("Received history entry requires primitive id and text fields");
                }
                received.add(new ChatHistoryEntry(
                        object.get("id").getAsInt(), object.get("text").getAsString()));
            }
            List<String> sent = new ArrayList<String>(sentArray.size());
            for (JsonElement sentElement : sentArray) {
                if (!sentElement.isJsonPrimitive()) {
                    throw new JsonParseException("Sent history entry must be a string");
                }
                sent.add(sentElement.getAsString());
            }
            return new ChatHistoryData(received, sent);
        } catch (Exception exception) {
            NfrUiEnhancements.LOGGER.error("Could not load chat history from {}", dataFile, exception);
            return ChatHistoryData.empty();
        }
    }

    @Override
    public boolean save(ChatHistoryData data) {
        if (data == null) throw new IllegalArgumentException("data must not be null");
        JsonObject root = new JsonObject();
        JsonArray received = new JsonArray();
        for (ChatHistoryEntry entry : data.received()) {
            JsonObject object = new JsonObject();
            object.addProperty("id", entry.id());
            object.addProperty("text", entry.json());
            received.add(object);
        }
        root.add("received", received);
        JsonArray sent = new JsonArray();
        for (String message : data.sent()) sent.add(new JsonPrimitive(message));
        root.add("sent", sent);

        Path temporary = dataFile.resolveSibling(dataFile.getFileName().toString() + ".tmp");
        try {
            Files.createDirectories(dataFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                writer.write(root.toString());
            }
            moveIntoPlace(temporary);
            return true;
        } catch (IOException exception) {
            NfrUiEnhancements.LOGGER.error("Could not save chat history to {}", dataFile, exception);
            return false;
        }
    }

    private void moveIntoPlace(Path temporary) throws IOException {
        try {
            Files.move(
                    temporary,
                    dataFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, dataFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static JsonArray requiredArray(JsonObject root, String name) {
        if (!root.has(name) || !root.get(name).isJsonArray()) {
            throw new JsonParseException("Chat history requires array field: " + name);
        }
        return root.getAsJsonArray(name);
    }
}
