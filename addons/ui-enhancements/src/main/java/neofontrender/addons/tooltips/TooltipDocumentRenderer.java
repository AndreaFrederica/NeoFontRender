package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import neofontrender.addons.build.UiBuildFeatures;

import java.util.concurrent.atomic.AtomicLong;

/** Common renderer for coordinate-free tooltip documents from every integration. */
final class TooltipDocumentRenderer {
    private static final long DIAGNOSTIC_INTERVAL_NANOS = 2_000_000_000L;
    private static final AtomicLong LAST_DIAGNOSTIC = new AtomicLong();
    private static final ThreadLocal<Boolean> IN_RENDER = new ThreadLocal<>();

    private TooltipDocumentRenderer() {}

    static boolean render(NfrTooltipApi.TooltipDocument source, int mouseX, int mouseY,
                          FontRenderer font) {
        if (!TooltipConfig.enabled || !Arc3DRuntimeSupport.isAvailable()
                || ObscureTooltipCompat.shouldYieldToObscure()) return false;
        if (Boolean.TRUE.equals(IN_RENDER.get())) {
            if (UiBuildFeatures.DIAGNOSTIC_LOGS) {
                NfrUiEnhancements.LOGGER.warn(
                        "Tooltip document renderer re-entry detected; allowing native fallback");
            }
            return false;
        }
        IN_RENDER.set(Boolean.TRUE);
        try {
            NfrTooltipApi.TooltipDocument document = NfrTooltipApi.finalizeDocument(source);
            if (document == null) return false;
            boolean diagnostic = false;
            if (UiBuildFeatures.DIAGNOSTIC_LOGS) {
                diagnostic = shouldDiagnostic();
                if (diagnostic) {
                    NfrUiEnhancements.LOGGER.debug(
                            "Tooltip document render: stack={}, lines={}, nodes={}, mouse=({}, {})",
                            describeStack(document.stack), document.lines.size(), document.nodes.size(),
                            mouseX, mouseY);
                }
            }
            java.util.List<NfrTooltipApi.VisualNode> previous = TooltipVisualPlan.EXTERNAL.get();
            TooltipVisualPlan.EXTERNAL.set(document.nodes);
            try {
                ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
                RenderTooltipEvent.Pre event = new RenderTooltipEvent.Pre(document.stack,
                        document.lines, mouseX, mouseY, resolution.getScaledWidth(),
                        resolution.getScaledHeight(), -1, font);
                boolean rendered = new ModernTooltipRenderer().draw(
                        event, null, "vanilla", null, true);
                if (UiBuildFeatures.DIAGNOSTIC_LOGS && diagnostic) {
                    NfrUiEnhancements.LOGGER.debug(
                            "Tooltip document renderer result: rendered={}, externalNodes={}",
                            rendered, document.nodes.size());
                }
                return rendered;
            } finally {
                if (previous == null) TooltipVisualPlan.EXTERNAL.remove();
                else TooltipVisualPlan.EXTERNAL.set(previous);
            }
        } finally {
            IN_RENDER.remove();
        }
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) return "empty";
        try {
            return String.valueOf(stack.getItem().getRegistryName()) + '@' + stack.getItemDamage();
        } catch (RuntimeException error) {
            return stack.getItem().getClass().getName();
        }
    }

    private static boolean shouldDiagnostic() {
        if (!UiBuildFeatures.DIAGNOSTIC_LOGS) return false;
        if (!NfrUiEnhancements.LOGGER.isDebugEnabled()) return false;
        long now = System.nanoTime();
        long previous = LAST_DIAGNOSTIC.get();
        return now - previous >= DIAGNOSTIC_INTERVAL_NANOS
                && LAST_DIAGNOSTIC.compareAndSet(previous, now);
    }
}
