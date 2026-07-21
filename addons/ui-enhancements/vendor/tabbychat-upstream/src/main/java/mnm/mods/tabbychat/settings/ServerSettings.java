package mnm.mods.tabbychat.settings;

import com.google.gson.reflect.TypeToken;
import mnm.mods.tabbychat.ChatChannel;
import mnm.mods.tabbychat.Reference;
import mnm.mods.tabbychat.extra.filters.UserFilter;
import mnm.mods.util.IPUtils;
import mnm.mods.util.config.SettingsFile;
import mnm.mods.util.config.ValueList;
import mnm.mods.util.config.ValueMap;
import net.minecraft.client.Minecraft;
import neofontrender.addons.chat.NfrTabbySettingsBridge;

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
    private final String configKey;

    public ServerSettings(SocketAddress url) {
        super(Reference.MOD_ID + "/" + getIPForFileName(url), "server");
        configKey = getIPForFileName(url);
        generalFile = new File(Reference.MOD_ID + "/" + getIPForFileName(url) + "/config/generalserversettings.json");
        filtersFile = new File(Reference.MOD_ID + "/" + getIPForFileName(url) + "/config/filters.json");
        channelsFile = new File(Reference.MOD_ID + "/" + getIPForFileName(url) + "/config/channels.json");
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
        NfrTabbySettingsBridge.loadServer(this, configKey);
    }

    @Override
    public void saveConfig() {
        NfrTabbySettingsBridge.saveServer(this, configKey);
    }

}
