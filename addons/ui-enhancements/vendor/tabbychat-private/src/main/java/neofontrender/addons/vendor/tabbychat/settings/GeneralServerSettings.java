package neofontrender.addons.vendor.tabbychat.settings;

import neofontrender.addons.vendor.tabbychat.util.ChannelPatterns;
import neofontrender.addons.vendor.tabbychat.util.MessagePatterns;
import neofontrender.addons.vendor.tabbychat.foundation.config.Value;
import neofontrender.addons.vendor.tabbychat.foundation.config.ValueList;
import neofontrender.addons.vendor.tabbychat.foundation.config.ValueObject;

public class GeneralServerSettings extends ValueObject {

    public Value<Boolean> channelsEnabled = value(true);
    public Value<Boolean> pmEnabled = value(true);
    public Value<ChannelPatterns> channelPattern = value(ChannelPatterns.BRACKETS);
    public Value<MessagePatterns> messegePattern = value(MessagePatterns.WHISPERS);
    public Value<Boolean> useDefaultTab = value(true);
    public ValueList<String> ignoredChannels = list();
    public Value<String> defaultChannel = value("");
    public Value<String> channelCommand = value("");
    public Value<String> messageCommand = value("");
}
