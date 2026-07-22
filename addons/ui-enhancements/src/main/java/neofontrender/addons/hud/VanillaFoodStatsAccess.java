package neofontrender.addons.hud;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;

/** Reads only public vanilla food APIs when AppleCore is absent. */
final class VanillaFoodStatsAccess implements FoodStatsAccess {
    static final VanillaFoodStatsAccess INSTANCE = new VanillaFoodStatsAccess();

    private VanillaFoodStatsAccess() {}

    @Override
    public float exhaustion(EntityPlayer player) {
        return 0.0F;
    }

    @Override
    public float maximumExhaustion(EntityPlayer player) {
        return 4.0F;
    }

    @Override
    public FoodPreview preview(ItemStack stack, EntityPlayer player) {
        if (stack == null || !(stack.getItem() instanceof ItemFood)) return FoodPreview.NONE;
        ItemFood food = (ItemFood) stack.getItem();
        int hunger = food.func_150905_g(stack);
        float saturation = hunger * food.func_150906_h(stack) * 2.0F;
        return new FoodPreview(hunger, saturation);
    }
}
