package neofontrender.addons.tooltips;

import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

final class ModernTooltipHandler {
    private final ModernTooltipRenderer renderer = new ModernTooltipRenderer();
    private boolean warnedLegendary;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltip(RenderTooltipEvent.Pre event) {
        if (!TooltipConfig.enabled || !Arc3DRuntimeSupport.isAvailable() || event.isCanceled()) return;
        if (TooltipConfig.yieldToLegendaryTooltips && Loader.isModLoaded("legendarytooltips")) {
            if (!warnedLegendary) {
                warnedLegendary = true;
                TooltipModule.LOGGER.info("LegendaryTooltips detected; the modern tooltip module will not intercept tooltips");
            }
            return;
        }
        if (renderer.draw(event)) event.setCanceled(true);
    }
}
