package neofontrender.addons.chat;

import java.util.UUID;

public final class ChatMessageMetadata {
    public final long timestamp;
    public final ChatSource source;
    public final String playerName;
    public final UUID playerId;
    public final String privatePeer;
    public final boolean outgoing;
    public final String privateBody;

    public ChatMessageMetadata(long timestamp, ChatSource source, String playerName, UUID playerId) {
        this(timestamp, source, playerName, playerId, "");
    }

    public ChatMessageMetadata(long timestamp, ChatSource source, String playerName, UUID playerId,
                               String privatePeer) {
        this(timestamp, source, playerName, playerId, privatePeer, false, "");
    }

    public ChatMessageMetadata(long timestamp, ChatSource source, String playerName, UUID playerId,
                               String privatePeer, boolean outgoing, String privateBody) {
        this.timestamp = timestamp;
        this.source = source == null ? ChatSource.SERVER : source;
        this.playerName = playerName == null ? "" : playerName;
        this.playerId = playerId;
        this.privatePeer = privatePeer == null ? "" : privatePeer;
        this.outgoing = outgoing;
        this.privateBody = privateBody == null ? "" : privateBody;
    }
}
