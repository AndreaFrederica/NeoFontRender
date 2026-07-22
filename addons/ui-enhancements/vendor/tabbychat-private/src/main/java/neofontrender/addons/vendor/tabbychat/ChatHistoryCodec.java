package neofontrender.addons.vendor.tabbychat;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import neofontrender.addons.vendor.tabbychat.api.Message;
import net.minecraft.util.IChatComponent;

import java.util.Date;
import java.util.List;

/** Explicit codec for persisted chat messages and their vanilla components. */
public final class ChatHistoryCodec {

    private ChatHistoryCodec() {}

    public static JsonArray writeMessages(List<Message> messages) {
        JsonArray array = new JsonArray();
        for (Message message : messages) {
            JsonObject item = new JsonObject();
            String componentJson = IChatComponent.Serializer.func_150696_a(message.getMessage());
            item.add("message", new JsonParser().parse(componentJson));
            item.addProperty("id", message.getID());
            if (message.getDate() != null) {
                item.addProperty("date", message.getDate().getTime());
            }
            array.add(item);
        }
        return array;
    }

    public static List<Message> readMessages(JsonElement element, int counter) {
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException("messages must be a JSON array");
        }
        List<Message> messages = Lists.newArrayList();
        for (JsonElement value : element.getAsJsonArray()) {
            if (!value.isJsonObject()) {
                throw new IllegalArgumentException("message must be a JSON object");
            }
            JsonObject item = value.getAsJsonObject();
            JsonElement component = item.get("message");
            JsonElement id = item.get("id");
            if (component == null || id == null) {
                throw new IllegalArgumentException("message and id are required");
            }
            IChatComponent chat = IChatComponent.Serializer.func_150699_a(component.toString());
            Date date = item.has("date") ? new Date(item.get("date").getAsLong()) : null;
            messages.add(ChatMessage.restored(counter, chat, id.getAsInt(), date));
        }
        return messages;
    }
}
