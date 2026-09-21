package neofontrender.addons.tooltips;

import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import neofontrender.api.client.tooltip.NfrTooltipApi;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Adds the built-in rotating model preview for eligible item tooltips. */
final class BuiltinPreviewProvider implements NfrTooltipApi.DocumentProvider {
    static final BuiltinPreviewProvider INSTANCE = new BuiltinPreviewProvider();

    private BuiltinPreviewProvider() {}

    @Override
    public Optional<NfrTooltipApi.TooltipDocument> create(ItemStack stack, List<String> lines) {
        if (!TooltipConfig.itemPreviewEnabled && !TooltipConfig.armorPreviewEnabled) {
            return Optional.empty();
        }
        if (stack == null || stack.isEmpty() || stack.getItem() == null) {
            return Optional.empty();
        }
        Item item = stack.getItem();
        String itemId = String.valueOf(item.getRegistryName()).toLowerCase(java.util.Locale.ROOT);
        if (matches(TooltipConfig.previewBlacklist, itemId)) return Optional.empty();
        boolean explicitlyAllowed = matches(TooltipConfig.previewWhitelist, itemId);
        if (!TooltipConfig.previewWhitelist.isEmpty() && !explicitlyAllowed) return Optional.empty();
        PreviewStyleRegistry.Style style = PreviewStyleRegistry.INSTANCE.match(stack);
        NfrTooltipApi.PreviewRequest request = null;
        request = NfrTooltipApi.selectPreview(stack, lines).orElse(null);
        if (request != null && ((request.previewKind() == NfrTooltipApi.PreviewKind.ITEM_STACK
                && !TooltipConfig.itemPreviewEnabled)
                || (request.previewKind() == NfrTooltipApi.PreviewKind.ARMOR
                && !TooltipConfig.armorPreviewEnabled))) {
            request = null;
        }
        if (request == null && PreviewEquipment.armorSlot(stack) != null
                && TooltipConfig.armorPreviewEnabled) {
            EntityEquipmentSlot slot = PreviewEquipment.armorSlot(stack);
            String configuredModel = style == null ? TooltipConfig.armorPreviewModel
                    : style.armorModel(TooltipConfig.armorPreviewModel);
            String configuredMode = style == null ? TooltipConfig.armorPreviewMode
                    : style.armorMode(TooltipConfig.armorPreviewMode);
            NfrTooltipApi.ArmorPreviewModel model = "player".equalsIgnoreCase(configuredModel)
                    ? NfrTooltipApi.ArmorPreviewModel.PLAYER : NfrTooltipApi.ArmorPreviewModel.ARMOR_STAND;
            // Elytra and other chest-slot items render through player-specific layers.
            if (model == NfrTooltipApi.ArmorPreviewModel.ARMOR_STAND
                    && !(item instanceof ItemArmor)) {
                model = NfrTooltipApi.ArmorPreviewModel.PLAYER;
            }
            NfrTooltipApi.ArmorPreviewMode mode = "full_set".equalsIgnoreCase(configuredMode)
                    ? NfrTooltipApi.ArmorPreviewMode.FULL_SET : NfrTooltipApi.ArmorPreviewMode.SINGLE_PIECE;
            float scale = style == null ? 30.0F : style.scale(30.0F);
            float pitch = style == null ? 25.0F : style.pitch(25.0F);
            float rotationSpeed = style == null ? 20.0F : style.rotationSpeed(20.0F);
            int previewWidth = style == null ? TooltipConfig.armorPreviewWidth
                    : style.width(TooltipConfig.armorPreviewWidth);
            int previewHeight = style == null ? 64 : style.height(64);
            List<String> effects = style == null || !TooltipConfig.previewEffectsEnabled
                    ? Collections.<String>emptyList() : style.effects;
            if (mode == NfrTooltipApi.ArmorPreviewMode.FULL_SET) {
                java.util.ArrayList<ItemStack> equipment = armorEquipment(stack, slot);
                request = new NfrTooltipApi.ArmorPreviewRequest(stack, equipment, model, mode,
                        scale, pitch, rotationSpeed, previewWidth, previewHeight, effects,
                        style == null ? null : style.sound());
            } else {
                request = new NfrTooltipApi.ArmorPreviewRequest(stack, slot, model, mode,
                        scale, pitch, rotationSpeed, previewWidth, previewHeight, effects,
                        style == null ? null : style.sound());
            }
        } else if (request == null && TooltipConfig.itemPreviewEnabled
                && ("all".equals(TooltipConfig.itemPreviewScope)
                || isToolOrWeapon(stack) || style != null || explicitlyAllowed)) {
            float scale = style == null ? 2.75F : style.scale(2.75F);
            float pitch = style == null ? -30.0F : style.pitch(-30.0F);
            float roll = style == null ? -45.0F : style.roll(-45.0F);
            float rotationSpeed = style == null ? -20.0F : style.rotationSpeed(-20.0F);
            int previewWidth = style == null ? TooltipConfig.itemPreviewWidth
                    : style.width(TooltipConfig.itemPreviewWidth);
            // Shields have a broad rotated quad. Reserve a larger side cell so the model does
            // not spill into the inventory behind the tooltip.
            if (item instanceof ItemShield) previewWidth = Math.max(previewWidth, 44);
            int previewHeight = style == null ? 64 : style.height(64);
            List<String> effects = style == null || !TooltipConfig.previewEffectsEnabled
                    ? Collections.<String>emptyList() : style.effects;
            request = new NfrTooltipApi.ItemPreviewRequest(stack, scale, pitch, roll,
                    rotationSpeed, previewWidth, previewHeight, effects,
                    style == null ? null : style.sound());
        } else if (request == null) {
            return Optional.empty();
        }

        NfrTooltipApi.TooltipDocument document = NfrTooltipApi.TooltipDocument.builder(stack, lines)
                .add(new NfrTooltipApi.PreviewNode(request))
                .build();
        return Optional.of(document);
    }

    private static java.util.ArrayList<ItemStack> armorEquipment(ItemStack hovered,
                                                                  EntityEquipmentSlot hoveredSlot) {
        java.util.ArrayList<ItemStack> equipment = new java.util.ArrayList<>();
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getMinecraft();
        for (EntityEquipmentSlot equipmentSlot : EntityEquipmentSlot.values()) {
            if (equipmentSlot.getSlotType() != EntityEquipmentSlot.Type.ARMOR) continue;
            ItemStack value = minecraft.player == null
                    ? ItemStack.EMPTY : minecraft.player.getItemStackFromSlot(equipmentSlot);
            equipment.add(equipmentSlot == hoveredSlot
                    ? (hovered == null ? ItemStack.EMPTY : hovered.copy())
                    : (value == null ? ItemStack.EMPTY : value.copy()));
        }
        if (equipment.isEmpty()) equipment.add(hovered == null ? ItemStack.EMPTY : hovered.copy());
        return equipment;
    }

    static boolean isToolOrWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) return false;
        Item item = stack.getItem();
        if (item instanceof ItemTool || item instanceof ItemSword || item instanceof ItemBow
                || item instanceof ItemHoe || item instanceof ItemShears
                || item instanceof ItemFishingRod || item instanceof ItemShield) {
            return true;
        }
        try {
            java.util.Set<String> toolClasses = item.getToolClasses(stack);
            return toolClasses != null && !toolClasses.isEmpty();
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean matches(List<String> rules, String itemId) {
        for (String rule : rules) {
            if (rule == null) continue;
            String value = rule.trim().toLowerCase(java.util.Locale.ROOT);
            if (value.equals(itemId) || (value.endsWith(":*")
                    && itemId.startsWith(value.substring(0, value.length() - 1)))) return true;
        }
        return false;
    }
}
