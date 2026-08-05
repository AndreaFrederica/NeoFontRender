package neofontrender.addons.server;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import neofontrender.addons.chat.network.SelfMessageCapability;
import neofontrender.addons.flight.network.FlightRollNetwork;

@Mod(
        modid = NfrUiEnhancementsServer.MOD_ID,
        name = "Revo UI Server Companion",
        version = NfrUiEnhancementsServer.VERSION,
        dependencies = "after:neofontrender_ui_enhancements",
        serverSideOnly = true,
        acceptableRemoteVersions = "*",
        acceptedMinecraftVersions = "[1.12,1.13)"
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
