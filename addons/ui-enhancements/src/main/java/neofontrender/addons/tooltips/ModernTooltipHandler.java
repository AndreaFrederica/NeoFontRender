package neofontrender.addons.tooltips;

import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

final class ModernTooltipHandler {
    private static final String MODULAR_UI_PRE_EVENT =
            "com.cleanroommc.modularui.screen.RichTooltipEvent$Pre";
    private final ModernTooltipRenderer renderer = new ModernTooltipRenderer();
    private boolean warnedLegendary;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltip(RenderTooltipEvent.Pre event) {
        ThaumcraftTooltipCompat.Context thaumcraftContext =
                ThaumcraftTooltipCompat.consumeContext(event.getLines());
        boolean[] compactLines = thaumcraftContext == null ? null : thaumcraftContext.compact;
        if (!TooltipConfig.enabled || !Arc3DRuntimeSupport.isAvailable() || event.isCanceled()) return;
        // HEI's ingredient-grid tooltips post Pre for compatibility, but their item icons are
        // rendered after the event. Cancelling here would replace the text and silently lose the
        // grid, so let HEI finish the content while its dedicated mixin replaces only the panel.
        if (HeiTooltipCompat.isCustomTooltipActive()) return;
        if (TooltipConfig.yieldToLegendaryTooltips && Loader.isModLoaded("legendarytooltips")) {
            if (!warnedLegendary) {
                warnedLegendary = true;
                TooltipModule.LOGGER.info("LegendaryTooltips detected; the modern tooltip module will not intercept tooltips");
            }
            return;
        }
        if (renderer.draw(event, compactLines,
                compactLines == null ? "vanilla" : "thaumcraft", thaumcraftContext,
                preservesCallerState(event.getClass().getName()))) {
            event.setCanceled(true);
        }
    }

    static boolean preservesCallerState(String eventClassName) {
        return MODULAR_UI_PRE_EVENT.equals(eventClassName);
    }
}
