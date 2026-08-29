package neofontrender.addons.hud;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import neofontrender.addons.ui.NfrUiEnhancements;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional, reflection-only bridge to AppleCore's public API. */
final class AppleCoreCompat {
    private static boolean attempted;
    private static Field accessorField;
    private static Method getMaxHunger;
    private static Method getExhaustion;
    private static Method getMaxExhaustion;
    private static Method isFood;
    private static Method getFoodValuesForPlayer;
    private static Field foodHunger;
    private static Field foodSaturationModifier;

    private AppleCoreCompat() {}

    static float maximumHunger(EntityPlayer player) {
        Object value = invoke(getMaxHunger(), player);
        return value instanceof Number ? Math.max(1.0F, ((Number) value).floatValue()) : 20.0F;
    }

    static float exhaustion(EntityPlayer player, float fallback) {
        Object value = invoke(getExhaustion(), player);
        return value instanceof Number ? ((Number) value).floatValue() : fallback;
    }

    static float maximumExhaustion(EntityPlayer player) {
        Object value = invoke(getMaxExhaustion(), player);
        return value instanceof Number ? Math.max(0.001F, ((Number) value).floatValue()) : 4.0F;
    }

    /** Returns both dynamic AppleCore food values, or null when the stack is not food. */
    static FoodValue foodValue(ItemStack stack, EntityPlayer player) {
        ensure();
        if (accessorField == null || stack == null || stack.isEmpty()) return null;
        try {
            Object accessor = accessorField.get(null);
            if (accessor == null || !Boolean.TRUE.equals(isFood.invoke(accessor, stack))) return null;
            Object values = getFoodValuesForPlayer.invoke(accessor, stack, player);
            if (values == null) return null;
            return new FoodValue(((Number) foodHunger.get(values)).floatValue(),
                    ((Number) foodSaturationModifier.get(values)).floatValue());
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Method getMaxHunger() { ensure(); return getMaxHunger; }
    private static Method getExhaustion() { ensure(); return getExhaustion; }
    private static Method getMaxExhaustion() { ensure(); return getMaxExhaustion; }

    private static Object invoke(Method method, EntityPlayer player) {
        if (method == null || accessorField == null) return null;
        try {
            Object accessor = accessorField.get(null);
            return accessor == null ? null : method.invoke(accessor, player);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static synchronized void ensure() {
        if (attempted) return;
        attempted = true;
        if (!Loader.isModLoaded("applecore")) return;
        try {
            Class<?> api = Class.forName("squeek.applecore.api.AppleCoreAPI");
            Class<?> accessor = Class.forName("squeek.applecore.api.IAppleCoreAccessor");
            Class<?> values = Class.forName("squeek.applecore.api.food.FoodValues");
            accessorField = api.getField("accessor");
            getMaxHunger = accessor.getMethod("getMaxHunger", EntityPlayer.class);
            getExhaustion = accessor.getMethod("getExhaustion", EntityPlayer.class);
            getMaxExhaustion = accessor.getMethod("getMaxExhaustion", EntityPlayer.class);
            isFood = accessor.getMethod("isFood", ItemStack.class);
            getFoodValuesForPlayer = accessor.getMethod("getFoodValuesForPlayer", ItemStack.class, EntityPlayer.class);
            foodHunger = values.getField("hunger");
            foodSaturationModifier = values.getField("saturationModifier");
            NfrUiEnhancements.LOGGER.info("AppleCore hunger API detected; HUD bars will use its dynamic values");
        } catch (ReflectiveOperationException | LinkageError error) {
            accessorField = null;
            NfrUiEnhancements.LOGGER.warn("AppleCore is present but its hunger API could not be linked", error);
        }
    }

    static final class FoodValue {
        final float hunger;
        final float saturationModifier;

        private FoodValue(float hunger, float saturationModifier) {
            this.hunger = hunger;
            this.saturationModifier = saturationModifier;
        }
    }
}
