package mnm.mods.tabbychat;

import mnm.mods.tabbychat.api.Channel;
import mnm.mods.tabbychat.api.ChannelStatus;
import mnm.mods.util.IChatProxy;
import net.minecraft.util.text.ITextComponent;

public class TabbedChatProxy implements IChatProxy {

    public static final IChatProxy INSTANCE = new TabbedChatProxy();

    @Override
    public void addToChat(String strchannel, ITextComponent msg) {
        Channel channel = TabbyChat.getInstance().getChat().getChannel(strchannel);
        channel.addMessage(msg);
        channel.setStatus(ChannelStatus.UNREAD);
    }

}
