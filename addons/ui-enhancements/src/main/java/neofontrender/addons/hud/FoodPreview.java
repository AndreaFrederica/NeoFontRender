package neofontrender.addons.hud;

/** Immutable food restoration preview returned by vanilla and AppleCore adapters. */
final class FoodPreview {
    static final FoodPreview NONE = new FoodPreview(0, 0.0F);

    final int hunger;
    final float saturation;

    FoodPreview(int hunger, float saturation) {
        this.hunger = Math.max(0, hunger);
        this.saturation = Math.max(0.0F, saturation);
    }
}
