package neofontrender.addons.ui;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import neofontrender.addons.effects.ScreenEffectsModule;
import neofontrender.addons.chat.EnhancedChatModule;
import neofontrender.addons.hud.HudBarsModule;
import neofontrender.addons.input.TextInputModule;
import neofontrender.addons.scrolling.SmoothScrollingModule;
import neofontrender.addons.tooltips.TooltipModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/** Forge entry point for optional client-side UI enhancements. */
@Mod(
        modid = NfrUiEnhancements.MOD_ID,
        name = "NFR UI Enhancements",
        version = NfrUiEnhancements.VERSION,
        dependencies = "required-after:neofontrender@[0.3.5,)",
        acceptedMinecraftVersions = "[1.7.10]",
        acceptableRemoteVersions = "*")
public final class NfrUiEnhancements {
    public static final String MOD_ID = "neofontrender_ui_enhancements";
    public static final String VERSION = "0.1.0";
    public static final Logger LOGGER = LogManager.getLogger("NFR UI Enhancements");

    private static final List<UiEnhancementModule> MODULES = Arrays.asList(
            new SmoothScrollingModule(),
            new TextInputModule(),
            new ScreenEffectsModule(),
            new HudBarsModule(),
            new EnhancedChatModule(),
            new TooltipModule());

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
}
