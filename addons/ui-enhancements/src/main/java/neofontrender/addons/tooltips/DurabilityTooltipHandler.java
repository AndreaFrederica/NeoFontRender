package neofontrender.addons.tooltips;

import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.api.client.tooltip.NfrTooltipApi;

import java.util.ArrayList;
import java.util.List;

/** Adds the exact remaining/max durability to the common item tooltip text. */
final class DurabilityTooltipHandler {
    private static final String LABEL_KEY = "neofontrender.tooltip.durability";

    /**
     * ItemTooltipEvent is the source of truth for vanilla, JEI and HEI text lists.  Keeping the
     * injection here also makes the line available to custom tooltip renderers that do not post
     * RenderTooltipEvent.Pre (HEI is one example).
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTooltip(ItemTooltipEvent event) {
        if (!TooltipConfig.numericDurabilityEnabled) return;
        append(event.getToolTip(), event.getItemStack());
    }

    /**
     * Some integrations build a second list after ItemTooltipEvent.  The common UIE document
     * finalizer reasserts the line at the final render boundary without duplicating it.
     */
    static NfrTooltipApi.TooltipDocument finalizeDocument(NfrTooltipApi.TooltipDocument document) {
        if (!TooltipConfig.numericDurabilityEnabled || document == null) return document;
        List<String> lines = new ArrayList<>(document.lines);
        if (!append(lines, document.stack)) return document;
        return new NfrTooltipApi.TooltipDocument(document.stack, lines, document.nodes);
    }

    static boolean append(List<String> lines, ItemStack stack) {
        if (lines == null) return false;
        String line = formatLine(stack);
        if (line == null || containsDurabilityLine(lines)) return false;
        lines.add(line);
        return true;
    }

    static String formatLine(ItemStack stack) {
        int remaining = remainingDamage(stack);
        if (remaining < 0) return null;
        int max = stack.getMaxDamage();
        String label = AddonI18n.tr(LABEL_KEY);
        return TextFormatting.GRAY.toString() + label + ": "
                + TextFormatting.WHITE + remaining + " / " + max;
    }

    /** Returns remaining durability, or -1 when the stack has no durability bar. */
    static int remainingDamage(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isItemStackDamageable()) return -1;
        int max = stack.getMaxDamage();
        return max <= 0 ? -1 : Math.max(0, max - stack.getItemDamage());
    }

    private static boolean containsDurabilityLine(List<String> lines) {
        String label = AddonI18n.tr(LABEL_KEY);
        for (String line : lines) {
            if (line == null) continue;
            String plain = TextFormatting.getTextWithoutFormattingCodes(line);
            if (plain != null && plain.trim().startsWith(label + ":")) return true;
        }
        return false;
    }
}
