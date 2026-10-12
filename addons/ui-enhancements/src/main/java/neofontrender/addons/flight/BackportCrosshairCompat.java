package neofontrender.addons.flight;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.lang.reflect.Method;

/** Exact-ID adapters for 1.13+ content backported by explicitly listed mods. */
final class BackportCrosshairCompat {
    private BackportCrosshairCompat() {}

    static boolean isSpyglass(ItemStack stack) {
        return CrosshairItemCompat.matches(CrosshairItemCompat.Kind.SPYGLASS, stack);
    }

    static boolean isCrossbow(ItemStack stack) {
        return CrosshairItemCompat.matches(CrosshairItemCompat.Kind.CROSSBOW, stack);
    }

    static boolean isTrident(ItemStack stack) {
        return CrosshairItemCompat.matches(CrosshairItemCompat.Kind.TRIDENT, stack);
    }

    static boolean isRangedWeapon(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        if (isSpyglass(stack)) return false;
        return stack.getItem() instanceof ItemBow || isCrossbow(stack) || isTrident(stack)
                || CrosshairItemCompat.matches(CrosshairItemCompat.Kind.RANGED, stack);
    }

    static boolean isChargeWeapon(ItemStack stack) {
        return stack != null && stack.getItem() != null
                && (stack.getItem() instanceof ItemBow || isCrossbow(stack) || isTrident(stack));
    }

    /** Items whose action follows the player's look vector rather than a shoulder-camera ray. */
    static boolean usesPlayerAim(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        if (isRangedWeapon(stack) || isSpyglass(stack)) return true;
        Item item = stack.getItem();
        return item == Items.ender_pearl || item == Items.ender_eye || item == Items.snowball
                || item == Items.egg || item == Items.experience_bottle
                || item == Items.potionitem;
    }

    static float chargeProgress(ItemStack stack, EntityPlayer player, int usedTicks) {
        if (stack == null || stack.getItem() == null) return 0.0F;
        if (isCrossbow(stack)) {
            Float exposed = invokeProgress(stack, player);
            if (exposed != null) return clamp01(exposed);
            int maximum = stack.getMaxItemUseDuration();
            int duration = maximum > 3 && maximum < 200 ? maximum - 3 : 25;
            return clamp01(usedTicks / (float) duration);
        }
        if (isTrident(stack)) return clamp01(usedTicks / 10.0F);
        if (stack.getItem() instanceof ItemBow) {
            return clamp01(usedTicks / 20.0F);
        }
        return 0.0F;
    }

    static ItemStack findProjectile(ItemStack weapon, EntityPlayer player) {
        ItemStack loaded = loadedProjectile(weapon);
        if (loaded != null) return loaded;

        ItemStack exposed = invokeAmmoFinder(weapon, player);
        if (exposed != null) return exposed;

        ItemStack main = player.getCurrentEquippedItem();
        if (isVanillaCrossbowAmmo(main)) return main;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isVanillaCrossbowAmmo(stack)) return stack;
        }
        return null;
    }

    static int projectileCount(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        if (stack.stackSize > 1) return stack.stackSize;
        try {
            Method method = stack.getItem().getClass().getMethod("getCurrentAmmo", ItemStack.class);
            Object result = method.invoke(stack.getItem(), stack);
            if (result instanceof Number) return Math.max(0, ((Number) result).intValue());
        } catch (ReflectiveOperationException | LinkageError ignored) { }
        return stack.stackSize;
    }

    private static Float invokeProgress(ItemStack stack, EntityPlayer player) {
        try {
            Method method = stack.getItem().getClass().getMethod("getCrosshairState", ItemStack.class, EntityPlayer.class);
            Object result = method.invoke(stack.getItem(), stack, player);
            return result instanceof Number ? ((Number) result).floatValue() : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static ItemStack invokeAmmoFinder(ItemStack weapon, EntityPlayer player) {
        for (Class<?>[] signature : new Class<?>[][]{
                {EntityLivingBase.class, ItemStack.class}, {ItemStack.class, EntityLivingBase.class}}) {
            try {
                Method method = weapon.getItem().getClass().getMethod("findAmmo", signature);
                Object result = signature[0] == EntityLivingBase.class
                        ? method.invoke(weapon.getItem(), player, weapon)
                        : method.invoke(weapon.getItem(), weapon, player);
                if (result instanceof ItemStack) return (ItemStack) result;
            } catch (ReflectiveOperationException | LinkageError ignored) { }
        }
        return null;
    }

    private static ItemStack loadedProjectile(ItemStack weapon) {
        NBTTagCompound tag = weapon.getTagCompound();
        if (tag == null || !tag.hasKey("ChargedProjectiles", 9)) return null;
        NBTTagList list = tag.getTagList("ChargedProjectiles", 10);
        return list.tagCount() == 0 ? null : ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(0));
    }

    private static boolean isVanillaCrossbowAmmo(ItemStack stack) {
        return stack != null && stack.getItem() != null
                && (stack.getItem() == Items.arrow || stack.getItem() == Items.fireworks);
    }

    private static float clamp01(float value) { return Math.max(0.0F, Math.min(1.0F, value)); }
}
