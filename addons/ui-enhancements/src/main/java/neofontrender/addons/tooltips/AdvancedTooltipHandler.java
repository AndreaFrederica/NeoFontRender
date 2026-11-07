package neofontrender.addons.tooltips;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Self-contained advanced item information, independent of optional tooltip mods. */
final class AdvancedTooltipHandler {
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTooltip(ItemTooltipEvent event) {
        if (!TooltipConfig.advancedEnabled || !event.getFlags().isAdvanced()) return;
        if (TooltipConfig.advancedRequireCtrl && !GuiScreen.isCtrlKeyDown()) return;
        ItemStack stack = event.getItemStack();
        if (stack == null || stack.isEmpty()) return;
        List<String> lines = event.getToolTip();
        lines.add("§7  -" + AddonI18n.tr("neofontrender.tooltip.advanced.title") + ":");
        if (TooltipConfig.advancedOreDictionary) {
            lines.add("§7  -" + AddonI18n.tr("neofontrender.tooltip.advanced.oreDictionary") + ":");
            Set<String> names = new LinkedHashSet<>();
            for (int id : OreDictionary.getOreIDs(stack)) names.add(OreDictionary.getOreName(id));
            if (names.isEmpty()) lines.add("§7     " + AddonI18n.tr("neofontrender.tooltip.advanced.none"));
            else for (String name : names) lines.add("§7     " + name);
        }
        Item item = stack.getItem();
        if (TooltipConfig.advancedRegistryName) {
            lines.add("§7  -" + AddonI18n.tr("neofontrender.tooltip.advanced.registryName") + ":");
            lines.add("§7     " + item.getRegistryName());
        }
        if (TooltipConfig.advancedUnlocalizedName) {
            lines.add("§7  -" + AddonI18n.tr("neofontrender.tooltip.advanced.unlocalizedName") + ":");
            String base = unlocalized(item, stack, false);
            lines.add("§7     " + base);
            String variant = unlocalized(item, stack, true);
            if (variant != null && !variant.equals(base)) lines.add("§7     " + variant);
        }
        if (TooltipConfig.advancedMeta) {
            lines.add("§7  -" + AddonI18n.tr("neofontrender.tooltip.advanced.meta") + ":");
            lines.add("§7     " + stack.getMetadata() + (item.getMaxDamage(stack) > 0 ? "/" + item.getMaxDamage(stack) : ""));
        }
        if (TooltipConfig.advancedNbt) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null && !tag.isEmpty()) {
                lines.add("§7  -" + AddonI18n.tr("neofontrender.tooltip.advanced.nbt") + ":");
                if (TooltipConfig.advancedNbtRequireShift && !GuiScreen.isShiftKeyDown()) lines.add("§7     [" + AddonI18n.tr("neofontrender.tooltip.advanced.pressShift") + "]");
                else {
                    String value = tag.toString();
                    int limit = TooltipConfig.advancedNbtCharacterLimit;
                    if (limit > 0 && value.length() > limit) value = value.substring(0, limit) + "§7 (还有 " + (tag.toString().length() - limit) + " 个字符...)";
                    lines.add("§7     " + value);
                }
            }
        }
    }

    private static String unlocalized(Item item, ItemStack stack, boolean variant) {
        try {
            java.lang.reflect.Method m = item.getClass().getMethod(variant ? "getUnlocalizedName" : "getUnlocalizedName");
            return String.valueOf(m.invoke(item));
        } catch (ReflectiveOperationException ignored) { return item.getRegistryName() == null ? "" : item.getRegistryName().toString(); }
    }
}
