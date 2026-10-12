package neofontrender.addons.tooltips;

import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.Loader;

public final class ObscureTooltipCompat {
    private static final ThreadLocal<ItemStack> ACTIVE_STACK = new ThreadLocal<ItemStack>();

    private ObscureTooltipCompat() {}

    public static boolean shouldReplacePanel() {
        return Loader.isModLoaded("obscure_tooltips")
                && TooltipConfig.enabled
                && !TooltipConfig.yieldToObscureTooltips
                && !(TooltipConfig.yieldToLegendaryTooltips && Loader.isModLoaded("legendarytooltips"))
                && Arc3DRuntimeSupport.isAvailable();
    }

    /** Either preview option, or the title icon, transfers the whole tooltip to UIE. */
    public static boolean shouldBypassObscure() {
        return selectsNfrRenderer(TooltipConfig.enabled, Arc3DRuntimeSupport.isAvailable(),
                TooltipConfig.itemPreviewEnabled, TooltipConfig.armorPreviewEnabled,
                TooltipConfig.headerIconEnabled);
    }

    /** True while Obscure keeps ownership of the tooltip it draws itself. */
    public static boolean shouldYieldToObscure() {
        return Loader.isModLoaded("obscure_tooltips") && !shouldBypassObscure();
    }

    public static void drawPanel(int x, int y, int width, int height, ItemStack stack) {
        ACTIVE_STACK.set(stack);
        // Obscure's bounds tightly hug its content. Give the replacement panel the same small
        // breathing room as NFR's native layout without shifting Obscure's model/text/effects.
        int outset = 2;
        ModernTooltipRenderer.drawCompatibleBackground(
                x - outset, y - outset, width + outset * 2, height + outset * 2, stack);
    }

    /** Replaces Obscure's fading header line with the configured NFR divider. */
    public static boolean replaceSeparator(int x, int y, int width) {
        if (!shouldReplacePanel()) return false;
        ModernTooltipRenderer.drawCompatibleDivider(x, y, width, ACTIVE_STACK.get());
        ACTIVE_STACK.remove();
        return true;
    }

    /**
     * Selection predicate for the 1.12.2 document pipeline. Kept here so the shared ownership
     * rules stay in one place; the 1.7.10 renderer replaces Obscure's panel instead of taking the
     * whole tooltip over, which is what {@link #shouldReplacePanel()} decides.
     */
    static boolean selectsNfrRenderer(boolean modernEnabled, boolean runtimeAvailable,
                                      boolean itemPreview, boolean armorPreview) {
        return selectsNfrRenderer(modernEnabled, runtimeAvailable, itemPreview, armorPreview, false);
    }

    static boolean selectsNfrRenderer(boolean modernEnabled, boolean runtimeAvailable,
                                      boolean itemPreview, boolean armorPreview,
                                      boolean headerIcon) {
        return modernEnabled && runtimeAvailable && (itemPreview || armorPreview || headerIcon);
    }
}
