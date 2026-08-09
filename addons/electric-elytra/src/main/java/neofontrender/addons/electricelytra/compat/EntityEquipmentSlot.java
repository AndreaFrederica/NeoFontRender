package neofontrender.addons.electricelytra.compat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/** 1.7.10 armor-slot compatibility for the chest slot used by electric elytra items. */
public enum EntityEquipmentSlot {
    CHEST;

    public static ItemStack getChest(EntityLivingBase entity) {
        return entity == null ? null : entity.getEquipmentInSlot(1);
    }

    public static void setChest(EntityPlayer player, ItemStack stack) {
        player.setCurrentItemOrArmor(1, stack);
    }
}
