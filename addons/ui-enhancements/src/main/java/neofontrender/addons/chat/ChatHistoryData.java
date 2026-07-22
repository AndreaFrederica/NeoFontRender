package neofontrender.addons.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable storage snapshot containing received and sent chat history. */
final class ChatHistoryData {
    private final List<ChatHistoryEntry> received;
    private final List<String> sent;

    ChatHistoryData(List<ChatHistoryEntry> received, List<String> sent) {
        if (received == null) throw new IllegalArgumentException("received must not be null");
        if (sent == null) throw new IllegalArgumentException("sent must not be null");
        this.received = Collections.unmodifiableList(new ArrayList<ChatHistoryEntry>(received));
        this.sent = Collections.unmodifiableList(new ArrayList<String>(sent));
    }

    static ChatHistoryData empty() {
        return new ChatHistoryData(
                Collections.<ChatHistoryEntry>emptyList(), Collections.<String>emptyList());
    }

    List<ChatHistoryEntry> received() {
        return received;
    }

    List<String> sent() {
        return sent;
    }
}
