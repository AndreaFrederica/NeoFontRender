package neofontrender.addons.chat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Mutable, bounded history model independent of Minecraft runtime classes. */
final class ChatHistoryBuffer {
    private final List<ChatHistoryEntry> received = new ArrayList<ChatHistoryEntry>();
    private final List<String> sent = new ArrayList<String>();

    void replace(ChatHistoryData data, int limit) {
        if (data == null) throw new IllegalArgumentException("data must not be null");
        received.clear();
        sent.clear();
        received.addAll(data.received());
        sent.addAll(data.sent());
        trim(limit);
    }

    void recordReceived(int id, String json, int limit) {
        if (id != 0) {
            for (Iterator<ChatHistoryEntry> iterator = received.iterator(); iterator.hasNext();) {
                if (iterator.next().id() == id) iterator.remove();
            }
        }
        received.add(new ChatHistoryEntry(id, json));
        trim(limit);
    }

    void recordSent(String message, int limit) {
        if (message == null) throw new IllegalArgumentException("message must not be null");
        if (sent.isEmpty() || !sent.get(sent.size() - 1).equals(message)) sent.add(message);
        trim(limit);
    }

    void trim(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        if (received.size() > limit) received.subList(0, received.size() - limit).clear();
        if (sent.size() > limit) sent.subList(0, sent.size() - limit).clear();
    }

    ChatHistoryData snapshot(boolean includeReceived, boolean includeSent) {
        List<ChatHistoryEntry> receivedSnapshot = includeReceived
                ? received : new ArrayList<ChatHistoryEntry>();
        List<String> sentSnapshot = includeSent ? sent : new ArrayList<String>();
        return new ChatHistoryData(receivedSnapshot, sentSnapshot);
    }
}
