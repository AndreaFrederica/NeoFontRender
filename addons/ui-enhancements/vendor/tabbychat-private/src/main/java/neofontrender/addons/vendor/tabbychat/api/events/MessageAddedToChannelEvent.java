package neofontrender.addons.vendor.tabbychat.api.events;

import neofontrender.addons.vendor.tabbychat.api.Channel;
import net.minecraft.util.IChatComponent;
import cpw.mods.fml.common.eventhandler.Event;

public class MessageAddedToChannelEvent extends Event {

    public IChatComponent text;
    public int id;
    public final Channel channel;

    public MessageAddedToChannelEvent(IChatComponent text, int id, Channel channel) {
        this.text = text;
        this.id = id;
        this.channel = channel;
    }
}
