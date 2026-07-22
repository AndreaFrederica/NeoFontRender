package neofontrender.addons.vendor.tabbychat.extra.filters;

import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.api.Channel;
import neofontrender.addons.vendor.tabbychat.api.filters.Filter;
import neofontrender.addons.vendor.tabbychat.api.filters.FilterEvent;
import neofontrender.addons.vendor.tabbychat.util.MessagePatterns;

import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 * Base class for filters that just need to set the
 */
public class MessageFilter implements Filter {

    @Nonnull
    @Override
    public Pattern getPattern() {

        MessagePatterns messege = TabbyChat.getInstance().serverSettings.general.messegePattern.get();
        String pattern = String.format("(?:%s|%s)", messege.getOutgoing(), messege.getIncoming());
        return Pattern.compile(pattern);
    }

    @Override
    public void action(FilterEvent event) {

        if (TabbyChat.getInstance().serverSettings.general.pmEnabled.get()) {
            // 0 = whole message, 1 = outgoing recipient, 2 = incoming recipient
            String player = event.matcher.group(1);
            // For when it's an incoming message.
            if (player == null) {
                player = event.matcher.group(2);
            }
            Channel dest = TabbyChat.getInstance().getChat().getChannel(player, true);
            event.channels.add(dest);
        }
    }
}
