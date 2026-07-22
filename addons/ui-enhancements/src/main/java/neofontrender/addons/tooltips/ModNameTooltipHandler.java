package neofontrender.addons.tooltips;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Map;

/** Mod Name Tooltip-compatible item provenance line, integrated into the addon tooltip pipeline. */
final class ModNameTooltipHandler {
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTooltip(ItemTooltipEvent event) {
        if (!TooltipConfig.modNameEnabled) return;

        String modName = getModName(event.getItemStack());
        if (modName == null || ModNameTooltipSupport.containsModName(event.getToolTip(), modName)) return;
        event.getToolTip().add(ModNameTooltipSupport.format(TooltipConfig.modNameFormat) + modName);
    }

    @Nullable
    private static String getModName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Item item = stack.getItem();
        String modId = item.getCreatorModId(stack);
        if (modId == null) return null;
        Map<String, ModContainer> mods = Loader.instance().getIndexedModList();
        ModContainer container = mods.get(modId);
        return container == null ? null : container.getName();
    }

}
