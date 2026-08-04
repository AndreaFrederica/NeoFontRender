package neofontrender.addons.server;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import neofontrender.addons.chat.network.SelfMessageCapability;

@Mod(
        modid = NfrUiEnhancementsServer.MOD_ID,
        name = "NFR UI Enhancements Server Companion",
        version = NfrUiEnhancementsServer.VERSION,
        dependencies = "after:neofontrender_ui_enhancements",
        serverSideOnly = true,
        acceptableRemoteVersions = "*",
        acceptedMinecraftVersions = "[1.12,1.13)"
)
public final class NfrUiEnhancementsServer {
    public static final String MOD_ID = "neofontrender_ui_enhancements_server";
    public static final String VERSION = "0.1.1";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        SelfMessageCapability.initialize();
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
