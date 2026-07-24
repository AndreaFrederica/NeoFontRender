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

    private static String getModName(ItemStack stack) {
        if (stack == null) return null;
        Item item = stack.getItem();
        UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(item);
        if (identifier == null) return null;
        String modId = identifier.modId;
        Map<String, ModContainer> mods = Loader.instance().getIndexedModList();
        ModContainer container = mods.get(modId);
        return container == null ? null : container.getName();
    }

}
