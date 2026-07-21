package mnm.mods.tabbychat;

import mnm.mods.tabbychat.api.ChannelStatus;
import mnm.mods.tabbychat.core.GuiNewChatTC;
import neofontrender.addons.mixin.tabbychat.IGuiIngame;
import mnm.mods.tabbychat.extra.ChatAddonAntiSpam;
import mnm.mods.tabbychat.extra.ChatLogging;
import mnm.mods.tabbychat.extra.filters.FilterAddon;
import mnm.mods.tabbychat.extra.spell.Spellcheck;
import mnm.mods.tabbychat.gui.settings.GuiSettingsScreen;
import mnm.mods.tabbychat.settings.ServerSettings;
import mnm.mods.tabbychat.settings.TabbySettings;
import mnm.mods.util.DefaultChatProxy;
import mnm.mods.util.IChatProxy;
import mnm.mods.util.gui.config.SettingPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;
import neofontrender.client.gui.NeofontrenderConfigScreen;
import neofontrender.addons.chat.EnhancedChatConfigAccess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class TabbyChat {
    private IChatProxy chatProxy = new DefaultChatProxy();
    private static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    private ChatManager chatManager;
    private GuiNewChatTC chatGui;
    private Spellcheck spellcheck;

    public TabbySettings settings;
    public ServerSettings serverSettings;

    private final File dataFolder;
    private SocketAddress currentServer;

    private boolean updateChecked;

    public TabbyChat() {
        instance = this;
        dataFolder = new File(Minecraft.getMinecraft().gameDir, Reference.MOD_ID);
    }

    private static TabbyChat instance;

    public static TabbyChat getInstance() {
        if (instance == null) {
            instance = new TabbyChat();
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

    public void openSettings(SettingPanel<?> setting) {
        NeofontrenderConfigScreen.open(Minecraft.getMinecraft().currentScreen);
    }

    public InetSocketAddress getCurrentServer() {
        return (InetSocketAddress) currentServer;
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public void init() {
        LOGGER.info("TabbyChat initializing");
        // Set global settings
        settings = new TabbySettings();
        settings.loadConfig();
        //LiteLoader.getInstance().registerExposable(settings, null);

        spellcheck = new Spellcheck(getDataFolder());

        // Keeps the current language updated whenever it is changed.
        IReloadableResourceManager irrm = (IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager();
        irrm.registerReloadListener(spellcheck);
        MinecraftForge.EVENT_BUS.register(new PlayerLoginHandler());
        MinecraftForge.EVENT_BUS.register(new ChatAddonAntiSpam());
        MinecraftForge.EVENT_BUS.register(new FilterAddon());
        MinecraftForge.EVENT_BUS.register(new ChatLogging(new File("logs/chat")));

    }

    public void postInit() {
        LOGGER.info("TabbyChat initializing");
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

        if (EnhancedChatConfigAccess.tabbedChatEnabled()) {
            try {
                hookIntoChat(Minecraft.getMinecraft().ingameGUI);
            } catch (Exception e) {
                LOGGER.fatal("Unable to hook into chat.  This is bad.", e);
            }
        }
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
        TabbyChat.getInstance().getChat().saveing();
    }

    private void hookIntoChat(GuiIngame guiIngame) {
        if (!GuiNewChatTC.class.isAssignableFrom(guiIngame.getChatGUI().getClass())) {
            ((IGuiIngame) guiIngame).setPersistantChatGUI(chatGui);
            LOGGER.info("Successfully hooked into chat.");
        }
    }

}
