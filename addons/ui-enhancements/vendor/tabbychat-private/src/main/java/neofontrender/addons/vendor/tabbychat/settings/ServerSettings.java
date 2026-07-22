package neofontrender.addons.vendor.tabbychat.settings;

import neofontrender.addons.vendor.tabbychat.ChatChannel;
import neofontrender.addons.vendor.tabbychat.Reference;
import neofontrender.addons.vendor.tabbychat.SettingsCodec;
import neofontrender.addons.vendor.tabbychat.extra.filters.UserFilter;
import neofontrender.addons.vendor.tabbychat.foundation.IPUtils;
import neofontrender.addons.vendor.tabbychat.foundation.config.SettingsFile;
import neofontrender.addons.vendor.tabbychat.foundation.config.ValueList;
import neofontrender.addons.vendor.tabbychat.foundation.config.ValueMap;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;


public class ServerSettings extends SettingsFile {

    public GeneralServerSettings general = new GeneralServerSettings();
    public ValueList<UserFilter> filters = list();
    public ValueMap<ChatChannel> channels = map();
    public ValueMap<ChatChannel> pms = map();
    private final File generalFile;
    private final File filtersFile;
    private final File channelsFile;

    public ServerSettings(SocketAddress url) {
        super(Reference.MOD_ID + "/" + getIPForFileName(url), "server");
        generalFile = resolve("config/generalserversettings.json");
        filtersFile = resolve("config/filters.json");
        channelsFile = resolve("config/channels.json");
    }

    private static String getIPForFileName(SocketAddress addr) {
        String ip;
        if (Minecraft.getMinecraft().isSingleplayer()) {
            ip = "singleplayer";
        } else {
            String url = ((InetSocketAddress) addr).getHostName();
            ip = "multiplayer/" + IPUtils.parse(url).getFileSafeAddress();
        }
        return ip;
    }

    @Override
    public void loadConfig() {
        if (!generalFile.exists() && !filtersFile.exists() && !channelsFile.exists()) saveConfig();
        general = SettingsCodec.readServerGeneral(loadJson(generalFile));
        filters = SettingsCodec.readFilters(loadJson(filtersFile));
        channels = SettingsCodec.readChannels(loadJson(channelsFile), false);
        pms = SettingsCodec.readChannels(loadJson(channelsFile), true);
    }

    @Override
    public void saveConfig() {
        saveJson(generalFile, SettingsCodec.writeServerGeneral(general));
        saveJson(filtersFile, SettingsCodec.writeFilters(filters));
        saveJson(channelsFile, SettingsCodec.writeChannels(channels, pms));
    }

}
