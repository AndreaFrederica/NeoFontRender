package neofontrender.addons.chat;

import mnm.mods.tabbychat.TabbyChat;
import mnm.mods.tabbychat.settings.GeneralSettings;
import mnm.mods.tabbychat.util.TimeStamps;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.Date;

public final class ChatTimestampDecorator {
    private ChatTimestampDecorator() {}

    public static ITextComponent decorate(ITextComponent component) {
        if (component == null || ChatMessageMetadataRegistry.isTimestamped(component)) return component;
        GeneralSettings settings = TabbyChat.getInstance().settings.general;
        if (!EnhancedChatConfig.enabled || !settings.timestampChat.get()) return component;
        ChatMessageMetadata metadata = ChatMessageMetadataRegistry.get(component);
        long timestamp = metadata == null ? System.currentTimeMillis() : metadata.timestamp;
        TimeStamps style = settings.timestampStyle.get();
        TextFormatting color = settings.timestampColor.get();
        TextComponentString decorated = new TextComponentString(color + style.format(new Date(timestamp)) + " ");
        decorated.appendSibling(component);
        ChatMessageMetadataRegistry.copy(component, decorated);
        ChatMessageMetadataRegistry.markTimestamped(decorated);
        return decorated;
    }
}
