package neofontrender.addons.flight;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.lang.reflect.Method;

/** Exact-ID adapters for 1.13+ content backported by explicitly listed 1.12.2 mods. */
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
        if (stack == null || stack.isEmpty()) return false;
        if (isSpyglass(stack)) return false;
        return stack.getItem() instanceof ItemBow || isCrossbow(stack) || isTrident(stack)
                || CrosshairItemCompat.matches(CrosshairItemCompat.Kind.RANGED, stack);
    }

    static boolean isChargeWeapon(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.getItem() instanceof ItemBow || isCrossbow(stack) || isTrident(stack));
    }

    /** Items whose action follows the player's look vector rather than a shoulder-camera ray. */
    static boolean usesPlayerAim(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (isRangedWeapon(stack) || isSpyglass(stack)) return true;
        net.minecraft.item.Item item = stack.getItem();
        return item == Items.ENDER_PEARL || item == Items.ENDER_EYE || item == Items.SNOWBALL
                || item == Items.EGG || item == Items.EXPERIENCE_BOTTLE
                || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION;
    }

    static float chargeProgress(ItemStack stack, EntityPlayer player, int usedTicks) {
        if (stack == null || stack.isEmpty()) return 0.0F;
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
        if (!loaded.isEmpty()) return loaded;

        ItemStack exposed = invokeAmmoFinder(weapon, player);
        if (!exposed.isEmpty()) return exposed;

        ItemStack off = player.getHeldItemOffhand();
        if (isVanillaCrossbowAmmo(off)) return off;
        ItemStack main = player.getHeldItemMainhand();
        if (isVanillaCrossbowAmmo(main)) return main;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isVanillaCrossbowAmmo(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    static int projectileCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (stack.getCount() > 1) return stack.getCount();
        try {
            Method method = stack.getItem().getClass().getMethod("getCurrentAmmo", ItemStack.class);
            Object result = method.invoke(stack.getItem(), stack);
            if (result instanceof Number) return Math.max(0, ((Number) result).intValue());
        } catch (ReflectiveOperationException | LinkageError ignored) { }
        return stack.getCount();
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
        return ItemStack.EMPTY;
    }

    private static ItemStack loadedProjectile(ItemStack weapon) {
        NBTTagCompound tag = weapon.getTagCompound();
        if (tag == null || !tag.hasKey("ChargedProjectiles", 9)) return ItemStack.EMPTY;
        NBTTagList list = tag.getTagList("ChargedProjectiles", 10);
        return list.tagCount() == 0 ? ItemStack.EMPTY : new ItemStack(list.getCompoundTagAt(0));
    }

    private static boolean isVanillaCrossbowAmmo(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof net.minecraft.item.ItemArrow
                || stack.getItem() == Items.FIREWORKS);
    }

    private static float clamp01(float value) { return Math.max(0.0F, Math.min(1.0F, value)); }
}
