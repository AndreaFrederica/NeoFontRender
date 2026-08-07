package neofontrender.addons.flight;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;

/** Exact-ID adapters for 1.13+ content backported by explicitly listed 1.12.2 mods. */
final class BackportCrosshairCompat {
    private BackportCrosshairCompat() {}

    static boolean isSpyglass(ItemStack stack) {
        return false;
    }

    static boolean isCrossbow(ItemStack stack) {
        return false;
    }

    static boolean isTrident(ItemStack stack) {
        return false;
    }

    static boolean isRangedWeapon(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        return stack.getItem() instanceof ItemBow
                || CrosshairItemCompat.matches(CrosshairItemCompat.Kind.RANGED, stack);
    }

    static boolean isChargeWeapon(ItemStack stack) {
        return stack != null && stack.getItem() != null
                && stack.getItem() instanceof ItemBow;
    }

    /** Items whose action follows the player's look vector rather than a shoulder-camera ray. */
    static boolean usesPlayerAim(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        if (isRangedWeapon(stack)) return true;
        Item item = stack.getItem();
        return item == Items.ender_pearl || item == Items.ender_eye || item == Items.snowball
                || item == Items.egg || item == Items.experience_bottle
                || item == Items.potionitem;
    }

    static float chargeProgress(ItemStack stack, EntityPlayer player, int usedTicks) {
        if (stack == null || stack.getItem() == null) return 0.0F;
        if (stack.getItem() instanceof ItemBow) {
            return clamp01(usedTicks / 20.0F);
        }
        return 0.0F;
    }

    static ItemStack findProjectile(ItemStack weapon, EntityPlayer player) {
        ItemStack main = player.getCurrentEquippedItem();
        if (isVanillaArrow(main)) return main;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isVanillaArrow(stack)) return stack;
        }
        return null;
    }

    static int projectileCount(ItemStack stack) {
        return stack == null || stack.getItem() == null ? 0 : stack.stackSize;
    }

    private static boolean isVanillaArrow(ItemStack stack) {
        return stack != null && stack.getItem() == Items.arrow;
    }

    private static float clamp01(float value) { return Math.max(0.0F, Math.min(1.0F, value)); }
}
