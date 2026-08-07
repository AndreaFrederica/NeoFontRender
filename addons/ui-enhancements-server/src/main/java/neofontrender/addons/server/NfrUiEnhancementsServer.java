package neofontrender.addons.server;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import neofontrender.addons.chat.network.SelfMessageCapability;
import neofontrender.addons.flight.network.FlightRollNetwork;

@Mod(
        modid = NfrUiEnhancementsServer.MOD_ID,
        name = "Revo UI Server Companion",
        version = NfrUiEnhancementsServer.VERSION,
        dependencies = "after:neofontrender_ui_enhancements",
        acceptableRemoteVersions = "*",
        acceptedMinecraftVersions = "[1.7.10,1.7.10]"
)
public final class NfrUiEnhancementsServer {
    public static final String MOD_ID = "neofontrender_ui_enhancements_server";
    public static final String VERSION = "0.2.0";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        FlightRollServerConfig.load(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        SelfMessageCapability.initialize();
        FlightRollNetwork.initializeServer();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        ServerChatHistoryManager.INSTANCE.initialize(event.getServer());
        event.registerServerCommand(new GroupChatCommand());
        event.registerServerCommand(new MultiTargetMessageCommand());
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        ServerChatHistoryManager.INSTANCE.shutdown();
    }
}
