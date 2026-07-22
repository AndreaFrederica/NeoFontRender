package neofontrender.addons.hud;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import neofontrender.addons.hud.api.HudBarValue;

/** Converts food state and an injectable compatibility adapter into one normalized HUD sample. */
final class FoodBarValues {
    private static final float MAXIMUM_FOOD = 20.0F;

    private FoodBarValues() {}

    static HudBarValue create(float food, float saturation, ItemStack held, EntityPlayer player,
                              FoodStatsAccess access, int primaryColor, int saturationColor,
                              int previewColor, int depletionColor, String text) {
        if (access == null) throw new IllegalArgumentException("food stats access must not be null");
        FoodPreview preview = access.preview(held, player);
        float maximumExhaustion = access.maximumExhaustion(player);
        if (Float.isNaN(maximumExhaustion) || Float.isInfinite(maximumExhaustion)
                || maximumExhaustion <= 0.0F) {
            throw new IllegalStateException("maximum exhaustion must be positive and finite");
        }
        float exhaustionRatio = access.exhaustion(player) / maximumExhaustion;
        float depletion = Math.max(0.0F, Math.min(1.0F, exhaustionRatio)) * MAXIMUM_FOOD;
        return new HudBarValue(food, MAXIMUM_FOOD, saturation, preview.hunger, depletion,
                primaryColor, saturationColor, previewColor, depletionColor, text);
    }
}
