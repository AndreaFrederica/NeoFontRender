package neofontrender.addons.hud;

/** Potential hunger and saturation gains for the currently held food. */
final class FoodBarPreview {
    final float hunger;
    final float saturation;

    private FoodBarPreview(float hunger, float saturation) {
        this.hunger = hunger;
        this.saturation = saturation;
    }

    static FoodBarPreview calculate(float food, float maximum, float currentSaturation,
                                    float nutrition, float saturationModifier) {
        float hunger = Math.max(0.0F, Math.min(maximum - food, nutrition));
        float finalFood = Math.min(maximum, food + hunger);
        float potentialSaturation = 2.0F * Math.max(0.0F, nutrition)
                * Math.max(0.0F, saturationModifier);
        float finalSaturation = Math.min(maximum,
                Math.min(finalFood, currentSaturation + potentialSaturation));
        return new FoodBarPreview(hunger, Math.max(0.0F, finalSaturation - currentSaturation));
    }
}
