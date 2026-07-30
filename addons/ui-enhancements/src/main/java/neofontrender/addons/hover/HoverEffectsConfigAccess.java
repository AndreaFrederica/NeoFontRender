package neofontrender.addons.hover;

public final class HoverEffectsConfigAccess {
    private HoverEffectsConfigAccess() {}

    public static boolean buttonsEnabled() { return HoverEffectsConfig.enabled && HoverEffectsConfig.buttons; }
    public static boolean slotsEnabled() { return HoverEffectsConfig.enabled && HoverEffectsConfig.slots; }
    public static boolean jeiIngredientGridEnabled() { return slotsEnabled() && HoverEffectsConfig.jeiIngredientGrid; }
    public static boolean modularUiSlotsEnabled() { return slotsEnabled() && HoverEffectsConfig.modularUiSlots; }
    public static boolean modularUiThemeColor() { return HoverEffectsConfig.modularUiThemeColor; }
    public static int buttonEnterMillis() { return HoverEffectsConfig.buttonEnterMillis; }
    public static int buttonExitMillis() { return HoverEffectsConfig.buttonExitMillis; }
    public static int slotEnterMillis() { return HoverEffectsConfig.slotEnterMillis; }
    public static int slotExitMillis() { return HoverEffectsConfig.slotExitMillis; }
    public static int slotColor() { return HoverEffectsConfig.slotColor; }
}
