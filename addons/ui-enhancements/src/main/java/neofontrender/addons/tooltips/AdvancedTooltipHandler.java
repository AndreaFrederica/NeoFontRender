package neofontrender.addons.tooltips;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.oredict.OreDictionary;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Self-contained advanced item information, independent of optional tooltip mods. */
final class AdvancedTooltipHandler {
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTooltip(ItemTooltipEvent event) {
        // 1.7.10 publishes the event state as public fields instead of accessors.
        if (!TooltipConfig.advancedEnabled || !event.showAdvancedItemTooltips) return;
        if (TooltipConfig.advancedRequireCtrl && !GuiScreen.isCtrlKeyDown()) return;
        ItemStack stack = event.itemStack;
        if (stack == null) return;
        List<String> lines = event.toolTip;
        lines.add("§7  -" + AddonI18n.tr("neofontrender.tooltip.advanced.title") + ":");
        if (TooltipConfig.advancedOreDictionary) {
            lines.add("§7  -" + AddonI18n.tr("neofontrender.tooltip.advanced.oreDictionary") + ":");
            Set<String> names = new LinkedHashSet<String>();
            for (int id : OreDictionary.getOreIDs(stack)) names.add(OreDictionary.getOreName(id));
            if (names.isEmpty()) lines.add("§7     " + AddonI18n.tr("neofontrender.tooltip.advanced.none"));
            else for (String name : names) lines.add("§7     " + name);
        }
        Item item = stack.getItem();
        if (TooltipConfig.advancedRegistryName) {
            lines.add("§7  -" + AddonI18n.tr("neofontrender.tooltip.advanced.registryName") + ":");
            // 1.7.10 has no Item#getRegistryName; the item registry owns the namespaced name.
            lines.add("§7     " + Item.itemRegistry.getNameForObject(item));
        }
        if (TooltipConfig.advancedUnlocalizedName) {
            lines.add("§7  -" + AddonI18n.tr("neofontrender.tooltip.advanced.unlocalizedName") + ":");
            String base = item.getUnlocalizedName();
            lines.add("§7     " + base);
            String variant = item.getUnlocalizedName(stack);
            if (variant != null && !variant.equals(base)) lines.add("§7     " + variant);
        }
        if (TooltipConfig.advancedMeta) {
            lines.add("§7  -" + AddonI18n.tr("neofontrender.tooltip.advanced.meta") + ":");
            lines.add("§7     " + stack.getItemDamage()
                    + (item.getMaxDamage(stack) > 0 ? "/" + item.getMaxDamage(stack) : ""));
        }
        if (TooltipConfig.advancedNbt) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null && !tag.hasNoTags()) {
                lines.add("§7  -" + AddonI18n.tr("neofontrender.tooltip.advanced.nbt") + ":");
                if (TooltipConfig.advancedNbtRequireShift && !GuiScreen.isShiftKeyDown()) {
                    lines.add("§7     [" + AddonI18n.tr("neofontrender.tooltip.advanced.pressShift") + "]");
                } else {
                    String value = tag.toString();
                    int limit = TooltipConfig.advancedNbtCharacterLimit;
                    if (limit > 0 && value.length() > limit) {
                        // No locale key exists for the remaining-character hint; main ships the
                        // message inline, so the adaptation keeps that wording.
                        value = value.substring(0, limit) + "§7 (还有 " + (value.length() - limit) + " 个字符...)";
                    }
                    lines.add("§7     " + value);
                }
            }
        }
    }
}
