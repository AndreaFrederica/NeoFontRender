package neofontrender.addons.chat;

import mnm.mods.tabbychat.TabbyChat;
import mnm.mods.tabbychat.api.Channel;
import mnm.mods.tabbychat.api.Chat;
import mnm.mods.tabbychat.api.events.ChatMessageEvent.ChatReceivedEvent;
import net.minecraft.util.text.ITextComponent;
import neofontrender.addons.tooltips.AddonI18n;

import java.util.Arrays;
import java.util.List;

/** Routes classified messages into stable built-in source tabs and private conversations. */
public final class ChatSourceChannels {
    private static final String PLAYER = "__uie_source_player";
    private static final String SERVER = "__uie_source_server";
    private static final String PRIVATE = "__uie_source_private";
    private static final List<String> BUILT_INS = Arrays.asList(PLAYER, SERVER, PRIVATE);

    private ChatSourceChannels() {}

    public static void sync() {
        TabbyChat tabby = TabbyChat.getInstance();
        Chat chat = tabby.getChat();
        if (chat == null || tabby.serverSettings == null) return;
        if (!enabled()) {
            for (Channel channel : chat.getChannels()) {
                if (isSourceChannel(channel)) chat.removeChannel(channel);
            }
            return;
        }
        add(chat, PLAYER, "player");
        add(chat, SERVER, "server");
        add(chat, PRIVATE, "private");
    }

    public static void route(ChatReceivedEvent event, ITextComponent message) {
        if (event == null || message == null || !enabled()) return;
        sync();
        ChatMessageMetadata metadata = ChatMessageMetadataRegistry.get(message);
        if (metadata == null) return;
        Chat chat = TabbyChat.getInstance().getChat();
        Channel source = chat.getChannel(channelName(metadata.source), false);
        event.channels.add(source);
        if (metadata.source == ChatSource.PRIVATE && !metadata.privatePeer.isEmpty()) {
            event.channels.add(chat.getChannel(metadata.privatePeer, true));
        }
    }

    public static boolean isSourceChannel(Channel channel) {
        return channel != null && !channel.isPm() && BUILT_INS.contains(channel.getName());
    }

    public static boolean isPlayerChannel(Channel channel) {
        return named(channel, PLAYER);
    }

    public static boolean isServerChannel(Channel channel) {
        return named(channel, SERVER);
    }

    public static boolean isPrivateChannel(Channel channel) {
        return named(channel, PRIVATE);
    }

    private static boolean named(Channel channel, String name) {
        return channel != null && !channel.isPm() && name.equals(channel.getName());
    }

    private static boolean enabled() {
        return EnhancedChatConfigAccess.tabbedChatEnabled()
                && EnhancedChatConfig.sourceClassification;
    }

    private static void add(Chat chat, String name, String translationSuffix) {
        Channel channel = chat.getChannel(name, false);
        channel.setAlias(AddonI18n.tr(
                "neofontrender_ui_enhancements.chat.source." + translationSuffix));
        channel.setPrefix("");
        channel.setPrefixHidden(false);
        chat.addChannel(channel);
    }

    private static String channelName(ChatSource source) {
        switch (source) {
            case PLAYER: return PLAYER;
            case PRIVATE: return PRIVATE;
            default: return SERVER;
        }
    }
}
