package neofontrender.addons.chat;

/** Runtime metadata added to vanilla chat lines by the chat-head mixin. */
public interface ChatHeadLineMetadata {
    String nfrUi$getSenderName();
    boolean nfrUi$isFirstFragment();
}
