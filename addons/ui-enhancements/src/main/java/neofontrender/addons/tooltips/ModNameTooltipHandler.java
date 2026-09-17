package neofontrender.addons.tooltips;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import neofontrender.api.client.tooltip.NfrTooltipApi;

/** Mod Name Tooltip-compatible item provenance line, integrated into the addon tooltip pipeline. */
final class ModNameTooltipHandler {
    static NfrTooltipApi.TooltipDocument finalizeDocument(NfrTooltipApi.TooltipDocument document) {
        if (!TooltipConfig.modNameEnabled || document == null || document.stack == null
                || document.stack.isEmpty()) return document;
        String modName = getModName(document.stack);
        if (modName == null || ModNameTooltipSupport.containsModName(document.lines, modName)) return document;
        List<String> lines = new java.util.ArrayList<>(document.lines);
        lines.add(ModNameTooltipSupport.format(TooltipConfig.modNameFormat) + modName);
        return new NfrTooltipApi.TooltipDocument(document.stack, lines, document.nodes);
    }


    /**
     * Configured ownership-line index, or -1 when the option is off or the line is absent.
     * Integrations that relocate the ownership line themselves use this to derive the
     * visual anchor from the line's original position.
     */
    @Nullable
    static String getModName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Item item = stack.getItem();
        String modId = item.getCreatorModId(stack);
        if (modId == null) return null;
        Map<String, ModContainer> mods = Loader.instance().getIndexedModList();
        ModContainer container = mods.get(modId);
        return container == null ? null : container.getName();
    }

}
