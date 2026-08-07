package neofontrender.addons.chat;

import neofontrender.addons.vendor.tabbychat.ChatChannel;
import neofontrender.addons.vendor.tabbychat.api.Channel;

/** Resolves the send-and-stay behavior from the currently selected TabbyChat channel. */
public final class ChatKeepOpenPolicy {
    public enum Category { PUBLIC, PLAYER, SERVER, PRIVATE, CUSTOM }

    private ChatKeepOpenPolicy() {}

    public static Category category(Channel channel) {
        if (channel == null || channel == ChatChannel.DEFAULT_CHANNEL) return Category.PUBLIC;
        if (channel.isPm() || ChatSourceChannels.isPrivateChannel(channel)) return Category.PRIVATE;
        if (ChatSourceChannels.isPlayerChannel(channel)) return Category.PLAYER;
        if (ChatSourceChannels.isServerChannel(channel)) return Category.SERVER;
        return Category.CUSTOM;
    }

    public static boolean shouldKeepOpen(Channel channel) {
        switch (category(channel)) {
            case PLAYER: return EnhancedChatConfig.keepOpenPlayer;
            case SERVER: return EnhancedChatConfig.keepOpenServer;
            case PRIVATE: return EnhancedChatConfig.keepOpenPrivate;
            case CUSTOM: return EnhancedChatConfig.keepOpenCustom;
            default: return EnhancedChatConfig.keepOpenPublic;
        }
    }

    public static void set(Channel channel, boolean value) {
        switch (category(channel)) {
            case PLAYER: EnhancedChatConfig.keepOpenPlayer = value; break;
            case SERVER: EnhancedChatConfig.keepOpenServer = value; break;
            case PRIVATE: EnhancedChatConfig.keepOpenPrivate = value; break;
            case CUSTOM: EnhancedChatConfig.keepOpenCustom = value; break;
            default: EnhancedChatConfig.keepOpenPublic = value;
        }
        EnhancedChatConfig.save();
    }
}
