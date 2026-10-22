package neofontrender.addons.tooltips;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ItemZoomPresentationTest {
    @BeforeAll
    static void bootstrap() { net.minecraft.init.Bootstrap.register(); }

    @Test
    void categoriesIncludeForgeToolsAndWearableBlocks() {
        assertEquals(ItemZoomPresentation.Category.BLOCK, category(new ItemStack(Blocks.STONE)));
        assertEquals(ItemZoomPresentation.Category.EQUIPMENT, category(new ItemStack(Blocks.PUMPKIN)));
        assertEquals(ItemZoomPresentation.Category.EQUIPMENT, category(new ItemStack(Items.ELYTRA)));
        assertEquals(ItemZoomPresentation.Category.EQUIPMENT, category(new ItemStack(Items.DIAMOND_CHESTPLATE)));
        for (Item item : new Item[]{Items.IRON_HOE, Items.SHEARS, Items.FISHING_ROD, Items.BOW, Items.SHIELD}) {
            assertEquals(ItemZoomPresentation.Category.TOOL, category(new ItemStack(item)));
        }
        Item hammer = new Item() {
            @Override public Set<String> getToolClasses(ItemStack stack) {
                return Collections.singleton("hammer");
            }
        };
        assertEquals(ItemZoomPresentation.Category.TOOL, category(new ItemStack(hammer)));
        assertEquals(ItemZoomPresentation.Category.OTHER, category(new ItemStack(Items.APPLE)));
    }

    @Test
    void shortcutChangesOnlyTheHoveredCategoryAndSettingsCancelRestoresIt() {
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.zoomOverlayBlockMode = "3d";
            TooltipConfig.zoomOverlayToolMode = "2d";
            TooltipConfig.zoomOverlayEquipmentMode = "off";
            TooltipConfig.zoomOverlayOtherMode = "2d";
            TooltipConfig.zoomOverlayArmorModel = "player";
            TooltipConfig.zoomOverlayArmorMode = "full_set";
            TooltipConfig.zoomOverlayMotion = "sway";
            TooltipConfig.Snapshot settings = TooltipConfig.snapshot();
            assertEquals("2d", ItemZoomPresentation.toggleMode(ItemZoomPresentation.Category.BLOCK));
            assertEquals("3d", ItemZoomPresentation.toggleMode(ItemZoomPresentation.Category.EQUIPMENT));
            assertEquals("2d", TooltipConfig.zoomOverlayToolMode);
            assertEquals("2d", TooltipConfig.zoomOverlayOtherMode);
            TooltipConfig.zoomOverlayArmorModel = "follow";
            TooltipConfig.zoomOverlayArmorMode = "single_piece";
            TooltipConfig.zoomOverlayMotion = "spin";
            settings.restore();
            assertEquals("3d", TooltipConfig.zoomOverlayBlockMode);
            assertEquals("off", TooltipConfig.zoomOverlayEquipmentMode);
            assertEquals("player", TooltipConfig.zoomOverlayArmorModel);
            assertEquals("full_set", TooltipConfig.zoomOverlayArmorMode);
            assertEquals("sway", TooltipConfig.zoomOverlayMotion);
        } finally { original.restore(); }
    }

    @Test
    void categoryModesKeepExistingScopeAndBlacklistRules() {
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.zoomOverlayScope = "tools";
            TooltipConfig.zoomOverlayWhitelist = Collections.emptyList();
            TooltipConfig.zoomOverlayBlacklist = Collections.emptyList();
            assertFalse(ItemZoomOverlay.allowed(new ItemStack(Blocks.STONE)));
            assertTrue(ItemZoomOverlay.allowed(new ItemStack(Items.IRON_HOE)));
            assertTrue(ItemZoomOverlay.allowed(new ItemStack(Items.ELYTRA)));
            TooltipConfig.zoomOverlayWhitelist = Collections.singletonList("minecraft:stone");
            assertTrue(ItemZoomOverlay.allowed(new ItemStack(Blocks.STONE)));
            TooltipConfig.zoomOverlayBlacklist = Collections.singletonList("minecraft:*");
            assertFalse(ItemZoomOverlay.allowed(new ItemStack(Blocks.STONE)));
        } finally { original.restore(); }
    }

    @Test
    void wearableRequestsReuseStandAndPlayerLayersWithoutTooltipEnableGate() {
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.armorPreviewEnabled = false;
            TooltipConfig.armorPreviewModel = "player";
            TooltipConfig.armorPreviewMode = "single_piece";
            TooltipConfig.zoomOverlayArmorModel = "follow";
            TooltipConfig.zoomOverlayArmorMode = "follow";
            TooltipConfig.zoomOverlayRotation = false;
            ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
            NfrTooltipApi.ArmorPreviewRequest request = ItemZoomRenderer.armorRequest(chest, 96);
            assertEquals(NfrTooltipApi.ArmorPreviewModel.PLAYER, request.model());
            assertEquals(EntityEquipmentSlot.CHEST, request.slot());
            assertEquals(0, request.rotationSpeed());
            assertTrue(request.effects().isEmpty());
            TooltipConfig.zoomOverlayArmorModel = "armor_stand";
            assertEquals(NfrTooltipApi.ArmorPreviewModel.ARMOR_STAND,
                    ItemZoomRenderer.armorRequest(chest, 96).model());
            assertEquals(NfrTooltipApi.ArmorPreviewModel.ARMOR_STAND,
                    ItemZoomRenderer.armorRequest(new ItemStack(Items.ELYTRA), 96).model());
        } finally { original.restore(); }
    }

    @Test
    void headsAndOtherWearablesRespectFollowAndExplicitZoomModels() {
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.zoomOverlayArmorMode = "single_piece";
            List<ItemStack> wearables = new java.util.ArrayList<>();
            for (int type = 0; type <= 5; type++) wearables.add(new ItemStack(Items.SKULL, 1, type));
            wearables.add(new ItemStack(Blocks.PUMPKIN));
            wearables.add(new ItemStack(Items.ELYTRA));
            for (ItemStack stack : wearables) {
                TooltipConfig.zoomOverlayArmorModel = "follow";
                TooltipConfig.armorPreviewModel = "armor_stand";
                assertEquals(NfrTooltipApi.ArmorPreviewModel.ARMOR_STAND,
                        ItemZoomRenderer.armorRequest(stack, 96).model());
                TooltipConfig.armorPreviewModel = "player";
                assertEquals(NfrTooltipApi.ArmorPreviewModel.PLAYER,
                        ItemZoomRenderer.armorRequest(stack, 96).model());
                TooltipConfig.zoomOverlayArmorModel = "armor_stand";
                assertEquals(NfrTooltipApi.ArmorPreviewModel.ARMOR_STAND,
                        ItemZoomRenderer.armorRequest(stack, 96).model());
                TooltipConfig.armorPreviewModel = "armor_stand";
                TooltipConfig.zoomOverlayArmorModel = "player";
                assertEquals(NfrTooltipApi.ArmorPreviewModel.PLAYER,
                        ItemZoomRenderer.armorRequest(stack, 96).model());
            }
        } finally { original.restore(); }
    }

    @Test
    void fullOutfitReplacesHoveredSlotAndCopiesStacks() {
        ItemStack wornChest = new ItemStack(Items.IRON_CHESTPLATE);
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        ItemStack hovered = new ItemStack(Items.DIAMOND_CHESTPLATE);
        List<ItemStack> outfit = PreviewEquipment.outfit(hovered, EntityEquipmentSlot.CHEST,
                slot -> slot == EntityEquipmentSlot.HEAD ? helmet
                        : slot == EntityEquipmentSlot.CHEST ? wornChest : ItemStack.EMPTY);
        assertTrue(outfit.stream().anyMatch(stack -> stack.getItem() == Items.DIAMOND_CHESTPLATE));
        assertFalse(outfit.stream().anyMatch(stack -> stack.getItem() == Items.IRON_CHESTPLATE));
        ItemStack copiedHelmet = outfit.stream().filter(stack -> stack.getItem() == Items.IRON_HELMET)
                .findFirst().orElseThrow();
        assertNotSame(helmet, copiedHelmet);
        copiedHelmet.setItemDamage(5);
        assertEquals(0, helmet.getItemDamage());
    }

    @Test
    void invalidConfigurationHasUsableDefaults() {
        assertEquals("2d", TooltipConfig.normalizeZoomDisplayMode(" 2D ", "3d"));
        assertEquals("off", TooltipConfig.normalizeZoomDisplayMode("OFF", "3d"));
        assertEquals("3d", TooltipConfig.normalizeZoomDisplayMode("invalid", "3d"));
        assertEquals("2d", TooltipConfig.normalizeZoomDisplayMode(null, "2d"));
        assertEquals("follow", TooltipConfig.normalizeZoomArmorModel("invalid"));
        assertEquals("follow", TooltipConfig.normalizeZoomArmorMode(null));
        assertEquals("spin", TooltipConfig.normalizeZoomMotion("invalid"));
    }

    private static ItemZoomPresentation.Category category(ItemStack stack) {
        return ItemZoomPresentation.category(stack);
    }
}
