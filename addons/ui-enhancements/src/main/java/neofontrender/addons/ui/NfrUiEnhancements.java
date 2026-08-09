package neofontrender.addons.ui;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import neofontrender.addons.cjk.CjkTypographyModule;
import neofontrender.addons.chat.EnhancedChatModule;
import neofontrender.addons.effects.ScreenEffectsModule;
import neofontrender.addons.flight.FlightRollModule;
import neofontrender.addons.hover.HoverEffectsModule;
import neofontrender.addons.hud.HudBarsModule;
import neofontrender.addons.input.TextInputModule;
import neofontrender.addons.loading.ResourceReloadModule;
import neofontrender.addons.loading.WorldLoadingModule;
import neofontrender.addons.mainmenu.MainMenuModule;
import neofontrender.addons.scrolling.SmoothScrollingModule;
import neofontrender.addons.server.GroupChatCommand;
import neofontrender.addons.server.MultiTargetMessageCommand;
import neofontrender.addons.server.ServerChatHistoryManager;
import neofontrender.addons.tips.TipsModule;
import neofontrender.addons.tooltips.TooltipModule;
import neofontrender.addons.worldcreation.CreateWorldModule;
import neofontrender.addons.zoom.ZoomModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/** Forge entry point for optional client-side UI enhancements. */
@Mod(
        modid = NfrUiEnhancements.MOD_ID,
        name = NfrUiEnhancements.MOD_NAME,
        version = NfrUiEnhancements.VERSION,
        dependencies = "required-after:neofontrender@[0.3.5,);required-after:modularui2",
        guiFactory = "neofontrender.addons.ui.UiEnhancementsGuiFactory",
        acceptedMinecraftVersions = "[1.7.10]",
        acceptableRemoteVersions = "*")
public final class NfrUiEnhancements {
    public static final String MOD_ID = "neofontrender_ui_enhancements";
    public static final String MOD_NAME = "Revo UI";
    public static final String VERSION = "0.6.0";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    private static final List<UiEnhancementModule> MODULES = Arrays.asList(
            new SmoothScrollingModule(),
            new CjkTypographyModule(),
            new TextInputModule(),
            new ScreenEffectsModule(),
            new WorldLoadingModule(),
            new ResourceReloadModule(),
            new ZoomModule(),
            new FlightRollModule(),
            new HoverEffectsModule(),
            new MainMenuModule(),
            new CreateWorldModule(),
            new HudBarsModule(),
            new EnhancedChatModule(),
            new TooltipModule(),
            new TipsModule());

    /** Loads addon configuration during Forge pre-initialization. */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        UiEnhancementsConfig.open();
        UiEnhancementsInfoContributions.register();
        for (UiEnhancementModule module : MODULES) module.preInit();
    }

    /** Activates each configured module during Forge initialization. */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        for (UiEnhancementModule module : MODULES) module.init();
        TooltipModule.registerRuntimeHooks();
    }

    /** The client bundles an integrated server, so server-side persistence also runs here. */
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
