package neofontrender.addons.tooltips;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;
import org.apache.logging.log4j.Logger;

/** Loads modern tooltip rendering configuration. */
public final class TooltipModule implements UiEnhancementModule {
    static final Logger LOGGER = NfrUiEnhancements.LOGGER;

    @Override
    public void preInit() {
        TooltipConfig.load();
        Arc3DRuntimeSupport.verify();
    }

    @Override
    public void init() {
        NfrSettingsPageRegistry.register(new ModernTooltipSettingsPage());
    }

    /** Registers hooks that require an initialized Forge client runtime. */
    public static void registerRuntimeHooks() {
        MinecraftForge.EVENT_BUS.register(new TooltipModule());
        MinecraftForge.EVENT_BUS.register(new ModNameTooltipHandler());
        if (Loader.isModLoaded("NotEnoughItems")) {
            registerNeiCompatIfGtnhLibLoaded();
        }
    }

    @Optional.Method(modid = "NotEnoughItems")
    private static void registerNeiCompatIfGtnhLibLoaded() {
        if (Loader.isModLoaded("gtnhlib")) registerNeiCompat();
    }

    @Optional.Method(modid = "gtnhlib")
    private static void registerNeiCompat() {
        MinecraftForge.EVENT_BUS.register(new NeiTooltipCompat());
    }

    /** Preserves the scene before the GUI draws panels, widgets, or tooltips. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void captureMicaScene(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (TooltipConfig.enabled && "mica".equals(TooltipConfig.renderStyle)) {
            MicaBackdrop.captureScene();
        }
    }

    @SubscribeEvent
    public void screenChanged(GuiOpenEvent event) {
        MicaBackdrop.invalidateScene();
    }
}
