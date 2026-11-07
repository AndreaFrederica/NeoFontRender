package neofontrender.addons.chat;

import java.util.UUID;

/** Message adjacency is supplied by the active chat view, oldest neighbour first. */
final class ChatHeadGrouping {
    private ChatHeadGrouping() {}

    static boolean sameSender(UUID sender, ChatMessageMetadata metadata,
                              UUID olderSender, ChatMessageMetadata older) {
        boolean server = metadata != null ? metadata.source == ChatSource.SERVER : sender == null;
        boolean olderServer = older != null ? older.source == ChatSource.SERVER : olderSender == null;
        if (server || olderServer) return server && olderServer;
        if (metadata != null && older != null && metadata.source != older.source) return false;
        if (sender != null || olderSender != null) return sender != null && sender.equals(olderSender);
        return metadata != null && older != null && !metadata.playerName.isEmpty()
                && metadata.playerName.equalsIgnoreCase(older.playerName);
    }
}
