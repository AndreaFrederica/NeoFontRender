package neofontrender.addons.tooltips;

import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.api.client.tooltip.NfrTooltipApi;

import java.util.ArrayList;
import java.util.List;

final class ModernTooltipHandler {
    private static final String MODULAR_UI_PRE_EVENT =
            "com.cleanroommc.modularui.screen.RichTooltipEvent$Pre";
    private final ModernTooltipRenderer renderer = new ModernTooltipRenderer();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltip(RenderTooltipEvent.Pre event) {
        // AE2 native backend owns this event; prevent UIE from consuming it or re-entering.
        if (neofontrender.api.client.tooltip.NfrTooltipApi.isNativeBypass()) return;
        // ItemTooltipEvent is not the final construction stage for extensible tooltips such as
        // ModularUI RichTooltip. Reassert provenance placement after every builder has run.
        RenderTooltipEvent.Pre layoutEvent = reorderForLayout(event);
        ThaumcraftTooltipCompat.Context thaumcraftContext =
                ThaumcraftTooltipCompat.consumeContext(layoutEvent.getLines());
        boolean[] compactLines = thaumcraftContext == null ? null : thaumcraftContext.compact;
        if (!TooltipConfig.enabled || !Arc3DRuntimeSupport.isAvailable() || event.isCanceled()) return;
        // HEI's ingredient-grid tooltips post Pre for compatibility, but their item icons are
        // rendered after the event. Cancelling here would replace the text and silently lose the
        // grid, so let HEI finish the content while its dedicated mixin replaces only the panel.
        if (HeiTooltipCompat.isCustomTooltipActive()) return;
        if (renderer.draw(layoutEvent, compactLines,
                compactLines == null ? "vanilla" : "thaumcraft", thaumcraftContext,
                preservesCallerState(event.getClass().getName()))) {
            event.setCanceled(true);
        }
    }

    /**
     * Forge wraps RenderTooltipEvent lines in an unmodifiable view. Create a private event only
     * when the final layout needs the provenance line moved, leaving the source event untouched.
     */
    private static RenderTooltipEvent.Pre reorderForLayout(RenderTooltipEvent.Pre source) {
        List<String> lines = new ArrayList<>(source.getLines());
        if (!ModNameTooltipHandler.moveToEnd(source.getStack(), lines)) return source;
        return new RenderTooltipEvent.Pre(source.getStack(), lines, source.getX(), source.getY(),
                source.getScreenWidth(), source.getScreenHeight(), source.getMaxWidth(),
                source.getFontRenderer());
    }

    static boolean preservesCallerState(String eventClassName) {
        return MODULAR_UI_PRE_EVENT.equals(eventClassName);
    }
}
