package neofontrender.addons.ui;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import neofontrender.addons.tooltips.TooltipModule;
import neofontrender.addons.scrolling.SmoothScrollingModule;
import neofontrender.addons.input.TextInputModule;
import neofontrender.addons.effects.ScreenEffectsModule;
import neofontrender.addons.chat.EnhancedChatModule;
import neofontrender.addons.hud.HudBarsModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

@Mod(
        modid = NfrUiEnhancements.MOD_ID,
        name = "NFR UI Enhancements",
        version = NfrUiEnhancements.VERSION,
        dependencies = "required-after:neofontrender@[0.3.5,)",
        clientSideOnly = true,
        acceptedMinecraftVersions = "[1.12,1.13)"
)
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
            new TooltipModule()
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
