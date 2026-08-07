package neofontrender.addons.ui;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import neofontrender.Tags;
import neofontrender.addons.server.GroupChatCommand;
import neofontrender.addons.server.MultiTargetMessageCommand;
import neofontrender.addons.server.ServerChatHistoryManager;
import neofontrender.addons.tips.TipsModule;
import neofontrender.addons.tooltips.TooltipModule;
import neofontrender.addons.diagnostics.DiagnosticsModule;
import neofontrender.addons.scrolling.SmoothScrollingModule;
import neofontrender.addons.input.TextInputModule;
import neofontrender.addons.effects.ScreenEffectsModule;
import neofontrender.addons.chat.EnhancedChatModule;
import neofontrender.addons.hud.HudBarsModule;
import neofontrender.addons.loading.WorldLoadingModule;
import neofontrender.addons.loading.ResourceReloadModule;
import neofontrender.addons.worldcreation.CreateWorldModule;
import neofontrender.addons.mainmenu.MainMenuModule;
import neofontrender.addons.zoom.ZoomModule;
import neofontrender.addons.flight.FlightRollModule;
import neofontrender.addons.hover.HoverEffectsModule;
import neofontrender.addons.cjk.CjkTypographyModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

@Mod(
        modid = NfrUiEnhancements.MOD_ID,
        name = NfrUiEnhancements.MOD_NAME,
        version = NfrUiEnhancements.VERSION,
        dependencies = NfrUiEnhancements.DEPENDENCIES,
        guiFactory = "neofontrender.addons.ui.UiEnhancementsGuiFactory",
        clientSideOnly = true,
        acceptedMinecraftVersions = "[1.12,1.13)"
)
public final class NfrUiEnhancements {
    public static final String MOD_ID = "neofontrender_ui_enhancements";
    public static final String MOD_NAME = "Revo UI";
    public static final String VERSION = "0.6.0";
    public static final String DEPENDENCIES =
            "required-after:" + Tags.MOD_ID + "@[" + Tags.VERSION + ",);"
                    + "required-after:modularui@[3.1.6,);"
                    + "after:applecore;after:chunkpregenerator;after:classicbar;after:jei;"
                    + "after:legendarytooltips;after:obscure_tooltips;after:optifine;after:quark";
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
            new TipsModule(),
            new DiagnosticsModule()
    );

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        UiEnhancementsConfig.open();
        UiEnhancementsInfoContributions.register();
        MODULES.forEach(UiEnhancementModule::preInit);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MODULES.forEach(UiEnhancementModule::init);
    }

    // The client bundles an integrated server (single-player / Open to LAN), so the
    // server-side group-chat and persistence features run from this mod as well.
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
