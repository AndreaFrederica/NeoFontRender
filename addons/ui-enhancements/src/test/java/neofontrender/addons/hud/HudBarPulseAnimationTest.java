package neofontrender.addons.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudBarPulseAnimationTest {
    @Test
    void heldFoodOverlayUsesClassicBarSinePulse() {
        assertEquals(128, HudBarPulseAnimation.alpha(0L));
        assertEquals(255, HudBarPulseAnimation.alpha(523_598_776L));
        assertEquals(0, HudBarPulseAnimation.alpha(1_570_796_327L));
    }

    @Test
    void heldFoodPreviewIncludesNutritionAndPotentialSaturation() {
        FoodBarPreview preview = FoodBarPreview.calculate(17.0F, 20.0F, 0.0F, 8.0F, 0.8F);
        assertEquals(3.0F, preview.hunger);
        assertEquals(12.8F, preview.saturation, 0.001F);
    }

    @Test
    void potentialSaturationCannotExceedFoodAfterEating() {
        FoodBarPreview preview = FoodBarPreview.calculate(10.0F, 20.0F, 10.0F, 4.0F, 0.6F);
        assertEquals(4.0F, preview.hunger);
        assertEquals(4.0F, preview.saturation);
    }
}
