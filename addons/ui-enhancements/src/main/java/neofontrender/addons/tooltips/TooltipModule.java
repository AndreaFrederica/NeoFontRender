package neofontrender.addons.tooltips;

import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;
import org.apache.logging.log4j.Logger;

/** Modern tooltip feature module and its independent settings-page registration. */
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
        MinecraftForge.EVENT_BUS.register(new ModernTooltipHandler());
    }
}
