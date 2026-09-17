package neofontrender.addons.tooltips;

import net.minecraft.entity.EntityLiving;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

/** Resolves wearable equipment through Forge first, then vanilla fallbacks. */
final class PreviewEquipment {
    private PreviewEquipment() {}

    static EntityEquipmentSlot armorSlot(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) return null;
        EntityEquipmentSlot slot = null;
        try {
            slot = stack.getItem().getEquipmentSlot(stack);
        } catch (RuntimeException | LinkageError ignored) {
            // Some legacy items implement only vanilla equipment behavior.
        }
        if (isArmor(slot)) return slot;
        try {
            slot = EntityLiving.getSlotForItemStack(stack);
        } catch (RuntimeException | LinkageError ignored) {
            // Fall through to ItemArmor for unusual coremod environments.
        }
        if (isArmor(slot)) return slot;
        return stack.getItem() instanceof ItemArmor
                ? ((ItemArmor) stack.getItem()).armorType : null;
    }

    private static boolean isArmor(EntityEquipmentSlot slot) {
        return slot != null && slot.getSlotType() == EntityEquipmentSlot.Type.ARMOR;
    }
}
