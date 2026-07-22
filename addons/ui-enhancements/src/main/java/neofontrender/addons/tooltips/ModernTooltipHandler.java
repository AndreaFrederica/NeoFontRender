package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import java.util.List;

/** Bridges GuiScreen tooltip calls to the addon renderer without global mutable item state. */
public final class ModernTooltipHandler {
    private static final ThreadLocal<ItemStack> ITEM_CONTEXT = new ThreadLocal<ItemStack>();
    private static final ModernTooltipRenderer RENDERER = new ModernTooltipRenderer();

    private ModernTooltipHandler() {}

    public static void beginItem(ItemStack stack) {
        ITEM_CONTEXT.set(stack);
    }

    public static void endItem() {
        ITEM_CONTEXT.remove();
    }

    public static boolean draw(
            List<String> lines, int mouseX, int mouseY, int width, int height, FontRenderer font) {
        return TooltipConfig.enabled && Arc3DRuntimeSupport.isAvailable()
                && RENDERER.draw(lines, ITEM_CONTEXT.get(), mouseX, mouseY, width, height, font);
    }
}
