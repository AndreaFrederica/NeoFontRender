package neofontrender.addons.vendor.tabbychat.settings;

import neofontrender.addons.vendor.tabbychat.util.TimeStamps;
import neofontrender.addons.vendor.tabbychat.foundation.config.Value;
import neofontrender.addons.vendor.tabbychat.foundation.config.ValueObject;
import net.minecraft.util.EnumChatFormatting;

public class GeneralSettings extends ValueObject {

    public Value<Boolean> logChat = value(true);
    public Value<Boolean> splitLog = value(true);
    public Value<Boolean> timestampChat = value(false);
    public Value<TimeStamps> timestampStyle = value(TimeStamps.MILITARYSECONDS);
    public Value<EnumChatFormatting> timestampColor = value(EnumChatFormatting.WHITE);
    public Value<Boolean> antiSpam = value(false);
    public Value<Double> antiSpamPrejudice = value(0D);
    public Value<Boolean> unreadFlashing = value(true);
    public Value<Boolean> checkUpdates = value(true);
}
