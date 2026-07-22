package neofontrender.addons.vendor.tabbychat.api.filters;

import java.util.Set;
import java.util.regex.Matcher;

import com.google.common.collect.Sets;

import neofontrender.addons.vendor.tabbychat.api.Channel;
import net.minecraft.util.IChatComponent;

public class FilterEvent {

    public final Matcher matcher;
    public IChatComponent text;
    public Set<Channel> channels = Sets.newHashSet();

    public FilterEvent(Matcher matcher, Set<Channel> channels, IChatComponent text) {
        this.matcher = matcher;
        this.text = text;
        this.channels = channels;
    }
}
