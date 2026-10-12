package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import neofontrender.api.client.tooltip.NfrTooltipApi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Resolves wearable equipment through 1.7.10's slot numbering. 1.7.10 has no
 * {@code EntityEquipmentSlot}: the equipment index used by
 * {@link EntityLivingBase#getEquipmentInSlot(int)} runs 0 (held) to 4 (helmet), which
 * {@link NfrTooltipApi.ArmorSlot#equipmentIndex()} mirrors.
 */
final class PreviewEquipment {
    private PreviewEquipment() {}

    /** Both tooltip and zoom use the same wearable layers and outfit selection. */
    static NfrTooltipApi.ArmorPreviewRequest request(ItemStack stack, String modelName,
            String modeName, float scale, float pitch, float speed, int width, int height,
            List<String> effects, NfrTooltipApi.PreviewSound sound) {
        NfrTooltipApi.ArmorSlot slot = armorSlot(stack);
        if (slot == null) return null;
        // The armor preview also renders custom heads and elytra; wearable type must not
        // override the model selected by the settings or item-specific preview style.
        NfrTooltipApi.ArmorPreviewModel model = "player".equalsIgnoreCase(modelName)
                ? NfrTooltipApi.ArmorPreviewModel.PLAYER : NfrTooltipApi.ArmorPreviewModel.ARMOR_STAND;
        NfrTooltipApi.ArmorPreviewMode mode = "full_set".equalsIgnoreCase(modeName)
                ? NfrTooltipApi.ArmorPreviewMode.FULL_SET : NfrTooltipApi.ArmorPreviewMode.SINGLE_PIECE;
        if (mode == NfrTooltipApi.ArmorPreviewMode.FULL_SET) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            List<ItemStack> equipment = outfit(stack, slot,
                    worn -> player == null ? null : player.getEquipmentInSlot(worn.equipmentIndex()));
            return new NfrTooltipApi.ArmorPreviewRequest(stack, equipment, model, mode,
                    scale, pitch, speed, width, height, effects, sound);
        }
        return new NfrTooltipApi.ArmorPreviewRequest(stack, slot, model, mode,
                scale, pitch, speed, width, height, effects, sound);
    }

    /** Wearable layers in {@link NfrTooltipApi.ArmorSlot} order; an absent layer is null. */
    static List<ItemStack> outfit(ItemStack hovered, NfrTooltipApi.ArmorSlot hoveredSlot,
                                  Function<NfrTooltipApi.ArmorSlot, ItemStack> worn) {
        List<ItemStack> equipment = new ArrayList<>();
        for (NfrTooltipApi.ArmorSlot slot : NfrTooltipApi.ArmorSlot.values()) {
            ItemStack value = slot == hoveredSlot ? hovered : worn.apply(slot);
            equipment.add(value == null ? null : value.copy());
        }
        return equipment;
    }

    /**
     * Maps a stack onto the armor slot it would occupy. {@link EntityLiving#getArmorPosition(ItemStack)}
     * is 1.7.10's counterpart of the 1.12 {@code EntityEquipmentSlot} lookup: it returns the
     * equipment index for {@code ItemArmor} and for the head-worn pumpkin and skull.
     */
    static NfrTooltipApi.ArmorSlot armorSlot(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        int position = EntityLiving.getArmorPosition(stack);
        for (NfrTooltipApi.ArmorSlot slot : NfrTooltipApi.ArmorSlot.values()) {
            if (slot.equipmentIndex() == position) return slot;
        }
        return null;
    }
}
