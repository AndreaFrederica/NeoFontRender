package neofontrender.addons.chat;

import java.util.Objects;

/** One persisted received component and its optional replacement id. */
final class ChatHistoryEntry {
    private final int id;
    private final String json;

    ChatHistoryEntry(int id, String json) {
        if (json == null) throw new IllegalArgumentException("json must not be null");
        this.id = id;
        this.json = json;
    }

    int id() {
        return id;
    }

    String json() {
        return json;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ChatHistoryEntry)) return false;
        ChatHistoryEntry entry = (ChatHistoryEntry) other;
        return id == entry.id && json.equals(entry.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, json);
    }
}
