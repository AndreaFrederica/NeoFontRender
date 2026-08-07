package neofontrender.addons.chat;

import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.settings.GeneralSettings;
import neofontrender.addons.vendor.tabbychat.util.TimeStamps;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Date;

public final class ChatTimestampDecorator {
    private ChatTimestampDecorator() {}

    public static IChatComponent decorate(IChatComponent component) {
        if (component == null || ChatMessageMetadataRegistry.isTimestamped(component)) return component;
        GeneralSettings settings = TabbyChat.getInstance().settings.general;
        if (!EnhancedChatConfig.enabled || !settings.timestampChat.get()) return component;
        ChatMessageMetadata metadata = ChatMessageMetadataRegistry.get(component);
        long timestamp = metadata == null ? System.currentTimeMillis() : metadata.timestamp;
        TimeStamps style = settings.timestampStyle.get();
        EnumChatFormatting color = settings.timestampColor.get();
        ChatComponentText decorated = new ChatComponentText(color + style.format(new Date(timestamp)) + " ");
        decorated.appendSibling(component);
        ChatMessageMetadataRegistry.copy(component, decorated);
        ChatMessageMetadataRegistry.markTimestamped(decorated);
        return decorated;
    }
}
