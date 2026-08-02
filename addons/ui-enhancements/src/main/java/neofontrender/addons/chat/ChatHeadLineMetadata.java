package neofontrender.addons.chat;

import java.util.UUID;

/** Runtime metadata added to vanilla chat lines by the chat-head mixin. */
public interface ChatHeadLineMetadata {
    UUID nfrUi$getSenderId();
    boolean nfrUi$isFirstFragment();
    ChatMessageMetadata nfrUi$getMessageMetadata();
}
