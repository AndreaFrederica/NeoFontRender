package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import neofontrender.addons.ui.NfrUiEnhancements;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Public source-level bridge for AE2 Supergiant's native tooltip path. */
public final class NfrAe2TooltipApi {
    private static final long DIAGNOSTIC_INTERVAL_NANOS = 2_000_000_000L;
    private static final AtomicLong LAST_DIAGNOSTIC = new AtomicLong();
    private static final ThreadLocal<Boolean> IN_RENDER = new ThreadLocal<>();

    private NfrAe2TooltipApi() {}

    public static void register() {
        boolean alreadyRegistered = neofontrender.api.client.tooltip.NfrTooltipApi.isRegistered();
        neofontrender.api.client.tooltip.NfrTooltipApi.register((document, mouseX, mouseY, font) ->
                renderDocument(document, mouseX, mouseY, font));
        NfrUiEnhancements.LOGGER.info("Registered public tooltip renderer bridge (AE2 source adapter); previousRenderer={}",
                alreadyRegistered);
    }

    private static boolean renderDocument(neofontrender.api.client.tooltip.NfrTooltipApi.TooltipDocument document,
                                          int mouseX, int mouseY, FontRenderer font) {
        if (Boolean.TRUE.equals(IN_RENDER.get())) {
            NfrUiEnhancements.LOGGER.warn("Tooltip bridge re-entry detected; returning false to allow native fallback");
            return false;
        }
        IN_RENDER.set(Boolean.TRUE);
        try {
            return renderDocumentGuarded(document, mouseX, mouseY, font);
        } finally {
            IN_RENDER.remove();
        }
    }

    private static boolean renderDocumentGuarded(neofontrender.api.client.tooltip.NfrTooltipApi.TooltipDocument document,
                                                 int mouseX, int mouseY, FontRenderer font) {
        boolean diagnostic = shouldDiagnostic();
        if (diagnostic) {
            NfrUiEnhancements.LOGGER.info(
                    "Tooltip bridge render: stack={}, lines={}, nodes={}, mouse=({},{}), registered={}",
                    describeStack(document.stack),
                    document.lines.size(), document.nodes.size(), mouseX, mouseY,
                    neofontrender.api.client.tooltip.NfrTooltipApi.isRegistered());
            for (int i = 0; i < document.nodes.size(); i++) {
                neofontrender.api.client.tooltip.NfrTooltipApi.VisualNode node = document.nodes.get(i);
                NfrUiEnhancements.LOGGER.info("  node[{}]: class={}, kind={}, width={}, height={}",
                        i, node == null ? "null" : node.getClass().getName(),
                        node == null ? "null" : node.kind(),
                        node == null ? 0 : safeWidth(node, font),
                        node == null ? 0 : safeHeight(node, font));
            }
        }
        TooltipVisualPlan.EXTERNAL.set(document.nodes);
        try {
            ScaledResolution resolution = new ScaledResolution(net.minecraft.client.Minecraft.getMinecraft());
            RenderTooltipEvent.Pre event = new RenderTooltipEvent.Pre(document.stack, document.lines,
                    mouseX, mouseY, resolution.getScaledWidth(), resolution.getScaledHeight(), -1, font);
            boolean rendered = new ModernTooltipRenderer().draw(event, null, "vanilla", null, true);
            if (diagnostic) {
                NfrUiEnhancements.LOGGER.info("Tooltip bridge renderer result: rendered={}, externalNodes={}",
                        rendered, document.nodes.size());
            }
            return rendered;
        }
        finally { TooltipVisualPlan.EXTERNAL.remove(); }
    }

    private static int safeWidth(neofontrender.api.client.tooltip.NfrTooltipApi.VisualNode node,
                                 FontRenderer font) {
        try { return node.width(font); } catch (RuntimeException e) { return -1; }
    }

    private static int safeHeight(neofontrender.api.client.tooltip.NfrTooltipApi.VisualNode node,
                                  FontRenderer font) {
        try { return node.height(font); } catch (RuntimeException e) { return -1; }
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) return "empty";
        try { return String.valueOf(stack.getItem().getRegistryName()) + "@" + stack.getItemDamage(); }
        catch (RuntimeException e) { return stack.getItem().getClass().getName(); }
    }

    private static boolean shouldDiagnostic() {
        long now = System.nanoTime();
        long previous = LAST_DIAGNOSTIC.get();
        if (now - previous < DIAGNOSTIC_INTERVAL_NANOS) return false;
        return LAST_DIAGNOSTIC.compareAndSet(previous, now);
    }

    public static boolean render(ItemStack stack, List<String> lines, int mouseX, int mouseY,
                                 FontRenderer font) {
        return renderDocument(new neofontrender.api.client.tooltip.NfrTooltipApi.TooltipDocument(
                stack, lines, java.util.Collections.<neofontrender.api.client.tooltip.NfrTooltipApi.VisualNode>emptyList()),
                mouseX, mouseY, font);
    }
}
