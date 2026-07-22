package neofontrender.addons.vendor.tabbychat;

import neofontrender.addons.vendor.tabbychat.api.ChannelStatus;
import neofontrender.addons.vendor.tabbychat.core.GuiNewChatTC;
import neofontrender.addons.vendor.tabbychat.extra.ChatAddonAntiSpam;
import neofontrender.addons.vendor.tabbychat.extra.ChatLogging;
import neofontrender.addons.vendor.tabbychat.extra.filters.FilterAddon;
import neofontrender.addons.vendor.tabbychat.extra.spell.Spellcheck;
import neofontrender.addons.vendor.tabbychat.gui.settings.GuiSettingsScreen;
import neofontrender.addons.vendor.tabbychat.host.TabbyChatHost;
import neofontrender.addons.vendor.tabbychat.settings.ServerSettings;
import neofontrender.addons.vendor.tabbychat.settings.TabbySettings;
import neofontrender.addons.vendor.tabbychat.foundation.DefaultChatProxy;
import neofontrender.addons.vendor.tabbychat.foundation.IChatProxy;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.SettingPanel;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class TabbyChat {
    private static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);
    private static TabbyChat instance;

    private final TabbyChatHost host;
    private final File dataFolder;
    private IChatProxy chatProxy = new DefaultChatProxy();

    private ChatManager chatManager;
    private GuiNewChatTC chatGui;
    private Spellcheck spellcheck;

    public TabbySettings settings;
    public ServerSettings serverSettings;

    private SocketAddress currentServer;

    private boolean updateChecked;

    private TabbyChat(TabbyChatHost host) {
        this.host = host;
        this.dataFolder = new File(host.getGameDirectory(), Reference.MOD_ID);
    }

    public static synchronized TabbyChat start(TabbyChatHost host) {
        if (host == null) {
            throw new IllegalArgumentException("host");
        }
        if (instance != null) {
            throw new IllegalStateException("TabbyChat has already been started");
        }
        instance = new TabbyChat(host);
        instance.initialize();
        return instance;
    }

    public static TabbyChat getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TabbyChat has not been started by its host");
        }
        return instance;
    }

    public static Logger getLogger() {
        return LOGGER;
    }


    public ChatManager getChat() {
        return chatManager;
    }

    public GuiNewChatTC getChatGui() {
        return chatGui;
    }


    public Spellcheck getSpellcheck() {
        return spellcheck;
    }

    void openSettings(SettingPanel<?> setting) {
        GuiSettingsScreen screen = new GuiSettingsScreen(setting);
        host.displayGuiScreen(screen);
    }

    public InetSocketAddress getCurrentServer() {
        return (InetSocketAddress) currentServer;
    }

    public File getDataFolder() {
        return dataFolder;
    }

    private void initialize() {
        LOGGER.info("TabbyChat initializing");
        // Set global settings
        settings = new TabbySettings();
        settings.loadConfig();
        //LiteLoader.getInstance().registerExposable(settings, null);

        spellcheck = new Spellcheck(getDataFolder());

        // Keeps the current language updated whenever it is changed.
        host.registerResourceReloadListener(spellcheck);
        host.registerEventHandler(new PlayerLoginHandler());
        host.registerEventHandler(new ChatAddonAntiSpam());
        host.registerEventHandler(new FilterAddon());
        host.registerEventHandler(new ChatLogging(new File(dataFolder, "logs/chat")));

        postInitialize();
    }

    private void postInitialize() {
        LOGGER.info("TabbyChat initializing GUI");
        // gui related stuff should be done here
        chatManager = new ChatManager(this);
        // this is set here because status relies on `chatManager`.
        ChatChannel.DEFAULT_CHANNEL.setStatus(ChannelStatus.ACTIVE);
        chatGui = new GuiNewChatTC(Minecraft.getMinecraft(), chatManager);

        chatProxy = new TabbedChatProxy();
    }

    public void onJoin(SocketAddress remoteAddress) {
        if (remoteAddress == null) {
            currentServer = new InetSocketAddress("127.0.0.1", 25565);
            LOGGER.info(String.format("TabbyChat onJoin: [singleplayer] %s:%s", ((InetSocketAddress) currentServer).getHostName(), ((InetSocketAddress) currentServer).getPort()));
        } else {
            currentServer = remoteAddress;
            LOGGER.info(String.format("TabbyChat onJoin: [multiplayer] %s:%s", ((InetSocketAddress) currentServer).getHostName(), ((InetSocketAddress) currentServer).getPort()));
        }

        // Set server settings
        serverSettings = new ServerSettings(currentServer);
        serverSettings.loadConfig();
        //LiteLoader.getInstance().registerExposable(serverSettings, null);

        hookIntoChat();
        // load chat
        File conf = serverSettings.getFile().getParentFile();
        try {
            chatManager.loadFrom(conf);
        } catch (Exception e) {
            LOGGER.warn("Unable to load chat data.", e);
        }

        if (settings.general.checkUpdates.get() && !updateChecked) {
            //UpdateChecker.runUpdateCheck(TabbedChatProxy.INSTANCE, TabbyRef.getVersionData());
            updateChecked = true;
        }
    }

    public void onQuit() {
        settings.saveConfig();
        serverSettings.saveConfig();
        TabbyChat.getInstance().getChat().saveNow();
    }

    private void hookIntoChat() {
        host.installChatGui(chatGui);
        LOGGER.info("Successfully hooked into chat.");
    }

}
