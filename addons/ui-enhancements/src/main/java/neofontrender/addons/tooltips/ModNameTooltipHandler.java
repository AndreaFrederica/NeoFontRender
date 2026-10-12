package neofontrender.addons.tooltips;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import neofontrender.api.client.tooltip.NfrTooltipApi;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Mod Name Tooltip-compatible item provenance line, integrated into the addon tooltip pipeline. */
public final class ModNameTooltipHandler {
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTooltip(ItemTooltipEvent event) {
        if (!TooltipConfig.modNameEnabled) return;

        String modName = getModName(event.itemStack);
        if (modName == null || ModNameTooltipSupport.containsModName(event.toolTip, modName)) return;
        event.toolTip.add(ModNameTooltipSupport.format(TooltipConfig.modNameFormat) + modName);
    }

    /**
     * Reasserts the ownership line once every visual source has been composed, because
     * {@code ItemTooltipEvent} is not the final construction stage for extensible tooltips.
     */
    static NfrTooltipApi.TooltipDocument finalizeDocument(NfrTooltipApi.TooltipDocument document) {
        if (!TooltipConfig.modNameEnabled || document == null || document.stack == null
                || document.stack.getItem() == null) return document;
        String modName = getModName(document.stack);
        if (modName == null || ModNameTooltipSupport.containsModName(document.lines, modName)) {
            return document;
        }
        List<String> lines = new ArrayList<>(document.lines);
        lines.add(ModNameTooltipSupport.format(TooltipConfig.modNameFormat) + modName);
        return new NfrTooltipApi.TooltipDocument(document.stack, lines, document.nodes);
    }

    /**
     * Owning mod's display name, or null when the item has no owning mod. The retained layout uses
     * this to anchor registered visual content on the ownership row.
     */
    @Nullable
    static String getModName(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        Item item = stack.getItem();
        // 1.7.10 has no Item#getCreatorModId; the GameRegistry identifier owns the mod id.
        UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(item);
        if (identifier == null) return null;
        String modId = identifier.modId;
        Map<String, ModContainer> mods = Loader.instance().getIndexedModList();
        ModContainer container = mods.get(modId);
        return container == null ? null : container.getName();
    }

}
