package neofontrender.addons.tooltips;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

public final class ObscureTooltipCompat {
    private static final ThreadLocal<ItemStack> ACTIVE_STACK = new ThreadLocal<>();

    private ObscureTooltipCompat() {}

    public static boolean shouldReplacePanel() {
        return Loader.isModLoaded("obscure_tooltips")
                && TooltipConfig.enabled
                && !TooltipConfig.yieldToObscureTooltips
                && !(TooltipConfig.yieldToLegendaryTooltips && Loader.isModLoaded("legendarytooltips"))
                && Arc3DRuntimeSupport.isAvailable();
    }

    public static void drawPanel(int x, int y, int width, int height, ItemStack stack) {
        ACTIVE_STACK.set(stack);
        // Obscure's bounds tightly hug its content. Give the replacement panel the same small
        // breathing room as NFR's native layout without shifting Obscure's model/text/effects.
        int outset = 2;
        ModernTooltipRenderer.drawCompatibleBackground(
                x - outset, y - outset, width + outset * 2, height + outset * 2, stack);
    }

    /** Replaces Obscure's two-piece fading header line with NFR's configured divider. */
    public static boolean replaceSeparator(int x, int y, int width) {
        if (!shouldReplacePanel()) return false;
        ModernTooltipRenderer.drawCompatibleDivider(x, y, width, ACTIVE_STACK.get());
        ACTIVE_STACK.remove();
        return true;
    }
}
