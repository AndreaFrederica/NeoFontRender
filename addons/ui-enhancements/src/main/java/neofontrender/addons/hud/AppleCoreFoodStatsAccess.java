package neofontrender.addons.hud;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import squeek.applecore.api.AppleCoreAPI;
import squeek.applecore.api.IAppleCoreAccessor;
import squeek.applecore.api.food.FoodValues;

/** Uses AppleCore's published 1.7.10 API for exhaustion and player-specific food values. */
final class AppleCoreFoodStatsAccess implements FoodStatsAccess {
    @Override
    public float exhaustion(EntityPlayer player) {
        return accessor().getExhaustion(player);
    }

    @Override
    public float maximumExhaustion(EntityPlayer player) {
        return accessor().getMaxExhaustion(player);
    }

    @Override
    public FoodPreview preview(ItemStack stack, EntityPlayer player) {
        if (stack == null || !accessor().isFood(stack)) return FoodPreview.NONE;
        FoodValues values = accessor().getFoodValuesForPlayer(stack, player);
        if (values == null) throw new IllegalStateException("AppleCore returned null food values");
        return new FoodPreview(values.hunger, values.getSaturationIncrement());
    }

    private static IAppleCoreAccessor accessor() {
        IAppleCoreAccessor accessor = AppleCoreAPI.accessor;
        if (accessor == null) throw new IllegalStateException("AppleCore accessor is not initialized");
        return accessor;
    }
}
