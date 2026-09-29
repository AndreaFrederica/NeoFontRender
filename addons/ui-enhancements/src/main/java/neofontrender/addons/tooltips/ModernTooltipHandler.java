package neofontrender.addons.tooltips;

import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.api.client.tooltip.NfrTooltipApi;

final class ModernTooltipHandler {
    private static final String MODULAR_UI_PRE_EVENT =
            "com.cleanroommc.modularui.screen.RichTooltipEvent$Pre";
    private final ModernTooltipRenderer renderer = new ModernTooltipRenderer();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltip(RenderTooltipEvent.Pre event) {
        // AE2 native backend owns this event; prevent UIE from consuming it or re-entering.
        if (neofontrender.api.client.tooltip.NfrTooltipApi.isNativeBypass()) return;
        if (!TooltipConfig.enabled || !Arc3DRuntimeSupport.isAvailable() || event.isCanceled()) return;
        if (ObscureTooltipCompat.shouldYieldToObscure()) return;
        // HEI draws its item grid after Pre; cancelling its event would discard the grid.
        if (HeiTooltipCompat.isCustomTooltipActive()) return;
        // Ordinary inventories do not pass through a specialized GUI. Compose
        // all registered visual sources at the common Forge tooltip boundary.
        NfrTooltipApi.TooltipDocument document =
                NfrTooltipApi.composeDocument(event.getStack(), event.getLines());
        if (!document.nodes.isEmpty()) {
            if (NfrTooltipApi.render(document, event.getX(), event.getY(), event.getFontRenderer())) {
                event.setCanceled(true);
                return;
            }
        }
        // ItemTooltipEvent is not the final construction stage for extensible tooltips such as
        // ModularUI RichTooltip. Reassert provenance placement after every builder has run.
        neofontrender.api.client.tooltip.NfrTooltipApi.TooltipDocument finalized =
                NfrTooltipApi.finalizeDocument(NfrTooltipApi.TooltipDocument.builder(
                        event.getStack(), event.getLines()).build());
        RenderTooltipEvent.Pre layoutEvent = new RenderTooltipEvent.Pre(event.getStack(), finalized.lines,
                event.getX(), event.getY(), event.getScreenWidth(), event.getScreenHeight(),
                event.getMaxWidth(), event.getFontRenderer());
        ThaumcraftTooltipCompat.Context thaumcraftContext =
                ThaumcraftTooltipCompat.consumeContext(layoutEvent.getLines());
        boolean[] compactLines = thaumcraftContext == null ? null : thaumcraftContext.compact;
        if (renderer.draw(layoutEvent, compactLines,
                compactLines == null ? "vanilla" : "thaumcraft", thaumcraftContext,
                preservesCallerState(event.getClass().getName()))) {
            event.setCanceled(true);
        }
    }

    static boolean preservesCallerState(String eventClassName) {
        return MODULAR_UI_PRE_EVENT.equals(eventClassName);
    }
}
