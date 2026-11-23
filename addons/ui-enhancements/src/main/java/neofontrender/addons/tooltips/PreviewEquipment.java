package neofontrender.addons.tooltips;

import net.minecraft.entity.EntityLiving;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import neofontrender.api.client.tooltip.NfrTooltipApi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Resolves wearable equipment through Forge first, then vanilla fallbacks. */
final class PreviewEquipment {
    private PreviewEquipment() {}

    /** Both tooltip and zoom use the same wearable layers and outfit selection. */
    static NfrTooltipApi.ArmorPreviewRequest request(ItemStack stack, String modelName,
            String modeName, float scale, float pitch, float speed, int width, int height,
            List<String> effects, NfrTooltipApi.PreviewSound sound) {
        EntityEquipmentSlot slot = armorSlot(stack);
        if (slot == null) return null;
        // Armor stands also render custom heads and elytra; wearable type must not override
        // the model selected by the settings or item-specific preview style.
        NfrTooltipApi.ArmorPreviewModel model = "player".equalsIgnoreCase(modelName)
                ? NfrTooltipApi.ArmorPreviewModel.PLAYER : NfrTooltipApi.ArmorPreviewModel.ARMOR_STAND;
        NfrTooltipApi.ArmorPreviewMode mode = "full_set".equalsIgnoreCase(modeName)
                ? NfrTooltipApi.ArmorPreviewMode.FULL_SET : NfrTooltipApi.ArmorPreviewMode.SINGLE_PIECE;
        if (mode == NfrTooltipApi.ArmorPreviewMode.FULL_SET) {
            net.minecraft.entity.player.EntityPlayer player =
                    net.minecraft.client.Minecraft.getMinecraft().player;
            List<ItemStack> equipment = outfit(stack, slot,
                    equipmentSlot -> player == null ? ItemStack.EMPTY : player.getItemStackFromSlot(equipmentSlot));
            return new NfrTooltipApi.ArmorPreviewRequest(stack, equipment, model, mode,
                    scale, pitch, speed, width, height, effects, sound);
        }
        return new NfrTooltipApi.ArmorPreviewRequest(stack, slot, model, mode,
                scale, pitch, speed, width, height, effects, sound);
    }

    static List<ItemStack> outfit(ItemStack hovered, EntityEquipmentSlot hoveredSlot,
                                 Function<EntityEquipmentSlot, ItemStack> worn) {
        List<ItemStack> equipment = new ArrayList<>();
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (!isArmor(slot)) continue;
            ItemStack value = slot == hoveredSlot ? hovered : worn.apply(slot);
            equipment.add(value == null ? ItemStack.EMPTY : value.copy());
        }
        return equipment;
    }

    static EntityEquipmentSlot armorSlot(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) return null;
        EntityEquipmentSlot slot = null;
        try {
            slot = stack.getItem().getEquipmentSlot(stack);
        } catch (RuntimeException | LinkageError ignored) {
            // Some legacy items implement only vanilla equipment behavior.
        }
        if (isArmor(slot)) return slot;
        try {
            slot = EntityLiving.getSlotForItemStack(stack);
        } catch (RuntimeException | LinkageError ignored) {
            // Fall through to ItemArmor for unusual coremod environments.
        }
        if (isArmor(slot)) return slot;
        return stack.getItem() instanceof ItemArmor
                ? ((ItemArmor) stack.getItem()).armorType : null;
    }

    private static boolean isArmor(EntityEquipmentSlot slot) {
        return slot != null && slot.getSlotType() == EntityEquipmentSlot.Type.ARMOR;
    }
}
