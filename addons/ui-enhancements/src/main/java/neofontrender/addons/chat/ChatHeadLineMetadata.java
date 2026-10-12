package neofontrender.addons.chat;

import java.util.UUID;

/** Runtime metadata added to vanilla chat lines by the chat-head mixin. */
public interface ChatHeadLineMetadata {
    UUID nfrUi$getSenderId();
    boolean nfrUi$isFirstFragment();
    ChatMessageMetadata nfrUi$getMessageMetadata();

    /**
     * Account name used for skin lookups and head grouping. Minecraft 1.7.10 keys the player list
     * by name instead of UUID, so the classified metadata is the sender's identity on this branch.
     */
    default String nfrUi$getSenderName() {
        ChatMessageMetadata metadata = nfrUi$getMessageMetadata();
        if (metadata == null || metadata.playerName.isEmpty()) return null;
        return metadata.playerName;
    }
}
