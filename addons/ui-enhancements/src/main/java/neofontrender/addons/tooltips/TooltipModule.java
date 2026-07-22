package neofontrender.addons.tooltips;

import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.ui.UiEnhancementModule;
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
    public void init() {}
}
