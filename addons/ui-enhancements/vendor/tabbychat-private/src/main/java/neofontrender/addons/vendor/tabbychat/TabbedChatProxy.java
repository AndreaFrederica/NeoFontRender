package neofontrender.addons.vendor.tabbychat;

import neofontrender.addons.vendor.tabbychat.api.Channel;
import neofontrender.addons.vendor.tabbychat.api.ChannelStatus;
import neofontrender.addons.vendor.tabbychat.foundation.IChatProxy;
import net.minecraft.util.IChatComponent;

public class TabbedChatProxy implements IChatProxy {

    public static final IChatProxy INSTANCE = new TabbedChatProxy();

    @Override
    public void addToChat(String strchannel, IChatComponent msg) {
        Channel channel = TabbyChat.getInstance().getChat().getChannel(strchannel);
        channel.addMessage(msg);
        channel.setStatus(ChannelStatus.UNREAD);
    }

}
