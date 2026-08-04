package neofontrender.addons.chat;

import mnm.mods.tabbychat.api.Message;
import net.minecraft.client.gui.ChatLine;

final class ChatSearchEntry {
    final String text;
    final String lowerText;
    final ChatMessageMetadata metadata;
    final Message tabbyMessage;
    final ChatLine vanillaLine;

    ChatSearchEntry(String text, ChatMessageMetadata metadata) {
        this(text, metadata, null, null);
    }

    ChatSearchEntry(String text, ChatMessageMetadata metadata, Message tabbyMessage, ChatLine vanillaLine) {
        this.text = text == null ? "" : text;
        this.lowerText = this.text.toLowerCase(java.util.Locale.ROOT);
        this.metadata = metadata == null ? new ChatMessageMetadata(
                System.currentTimeMillis(), ChatSource.SERVER, "", null) : metadata;
        this.tabbyMessage = tabbyMessage;
        this.vanillaLine = vanillaLine;
    }
}
