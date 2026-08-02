package neofontrender.addons.chat;

final class ChatSearchEntry {
    final String text;
    final ChatMessageMetadata metadata;

    ChatSearchEntry(String text, ChatMessageMetadata metadata) {
        this.text = text == null ? "" : text;
        this.metadata = metadata == null ? new ChatMessageMetadata(
                System.currentTimeMillis(), ChatSource.SERVER, "", null) : metadata;
    }
}
