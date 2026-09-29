package neofontrender.addons.tooltips;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/** Category policy is shared by settings, hover capture and the mode shortcut. */
final class ItemZoomPresentation {
    enum Category { BLOCK, TOOL, EQUIPMENT, OTHER }

    private ItemZoomPresentation() {}

    static Category category(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Category.OTHER;
        if (PreviewEquipment.armorSlot(stack) != null) return Category.EQUIPMENT;
        if (stack.getItem() instanceof ItemBlock) return Category.BLOCK;
        if (BuiltinPreviewProvider.isToolOrWeapon(stack)) return Category.TOOL;
        return Category.OTHER;
    }

    static String mode(Category category) {
        switch (category) {
            case BLOCK: return TooltipConfig.zoomOverlayBlockMode;
            case TOOL: return TooltipConfig.zoomOverlayToolMode;
            case EQUIPMENT: return TooltipConfig.zoomOverlayEquipmentMode;
            default: return TooltipConfig.zoomOverlayOtherMode;
        }
    }

    static String toggleMode(Category category) {
        String mode = "3d".equals(mode(category)) ? "2d" : "3d";
        switch (category) {
            case BLOCK: TooltipConfig.zoomOverlayBlockMode = mode; break;
            case TOOL: TooltipConfig.zoomOverlayToolMode = mode; break;
            case EQUIPMENT: TooltipConfig.zoomOverlayEquipmentMode = mode; break;
            default: TooltipConfig.zoomOverlayOtherMode = mode;
        }
        return mode;
    }

    static boolean inScope(Category category, String scope) {
        return "all".equals(scope) || category == Category.TOOL || category == Category.EQUIPMENT;
    }

    static float yaw(Category category, float spin) {
        return (category == Category.BLOCK ? 45.0F : 0.0F) + spin;
    }

    static float pitch(Category category) {
        return category == Category.BLOCK ? 25.0F : category == Category.TOOL ? 30.0F : 15.0F;
    }

    static float roll(Category category) {
        // Model-space Y points up. +45 degrees stands the diagonal sword blade upright.
        return category == Category.TOOL ? 45.0F : 0.0F;
    }
}
