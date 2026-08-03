package neofontrender.addons.ui;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import neofontrender.Tags;
import neofontrender.addons.tips.TipsModule;
import neofontrender.addons.tooltips.TooltipModule;
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
import neofontrender.addons.hover.HoverEffectsModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

@Mod(
        modid = NfrUiEnhancements.MOD_ID,
        name = "NFR UI Enhancements",
        version = NfrUiEnhancements.VERSION,
        dependencies = NfrUiEnhancements.DEPENDENCIES,
        guiFactory = "neofontrender.addons.ui.UiEnhancementsGuiFactory",
        clientSideOnly = true,
        acceptedMinecraftVersions = "[1.12,1.13)"
)
public final class NfrUiEnhancements {
    public static final String MOD_ID = "neofontrender_ui_enhancements";
    public static final String VERSION = "0.2.3";
    public static final String DEPENDENCIES =
            "required-after:" + Tags.MOD_ID + "@[" + Tags.VERSION + ",);"
                    + "required-after:modularui@[3.1.6,);"
                    + "after:applecore;after:chunkpregenerator;after:classicbar;after:jei;"
                    + "after:legendarytooltips;after:obscure_tooltips;after:optifine;after:quark";
    public static final Logger LOGGER = LogManager.getLogger("NFR UI Enhancements");

    private static final List<UiEnhancementModule> MODULES = Arrays.asList(
            new SmoothScrollingModule(),
            new TextInputModule(),
            new ScreenEffectsModule(),
            new WorldLoadingModule(),
            new ResourceReloadModule(),
            new ZoomModule(),
            new HoverEffectsModule(),
            new MainMenuModule(),
            new CreateWorldModule(),
            new HudBarsModule(),
            new EnhancedChatModule(),
            new TooltipModule(),
            new TipsModule()
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
}
