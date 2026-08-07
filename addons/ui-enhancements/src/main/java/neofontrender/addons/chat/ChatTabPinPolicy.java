package neofontrender.addons.chat;

import neofontrender.addons.vendor.tabbychat.ChatChannel;
import neofontrender.addons.vendor.tabbychat.api.Channel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pinning policy for chat tabs. Pinned channels are kept at the front of the
 * tray in the order they were pinned; the order doubles as their priority.
 * The list is persisted in {@link EnhancedChatConfig} across sessions.
 */
public final class ChatTabPinPolicy {
    private ChatTabPinPolicy() {}

    public static boolean isPinned(Channel channel) {
        return channel != null && isPinned(channel.getName());
    }

    public static boolean isPinned(String channelName) {
        return parse(EnhancedChatConfig.pinnedTabs).contains(channelName);
    }

    public static void setPinned(Channel channel, boolean pinned) {
        if (channel == null || channel == ChatChannel.DEFAULT_CHANNEL) return;
        Set<String> names = parse(EnhancedChatConfig.pinnedTabs);
        if (pinned) {
            names.add(channel.getName());
        } else {
            names.remove(channel.getName());
        }
        EnhancedChatConfig.pinnedTabs = String.join(",", names);
        EnhancedChatConfig.save();
    }

    /** Pinned channels first (in pin order), then the rest in original order. */
    public static List<Channel> ordered(List<Channel> channels) {
        Set<String> pinned = parse(EnhancedChatConfig.pinnedTabs);
        List<Channel> result = new ArrayList<>();
        for (String name : pinned) {
            for (Channel channel : channels) {
                if (name.equals(channel.getName())) result.add(channel);
            }
        }
        for (Channel channel : channels) {
            if (!pinned.contains(channel.getName())) result.add(channel);
        }
        return result;
    }

    private static Set<String> parse(String value) {
        Set<String> names = new LinkedHashSet<>();
        if (value == null || value.trim().isEmpty()) return names;
        for (String name : value.split(",")) {
            if (!name.trim().isEmpty()) names.add(name.trim());
        }
        return names;
    }
}
