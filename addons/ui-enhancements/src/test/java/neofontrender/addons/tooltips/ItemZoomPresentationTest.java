package neofontrender.addons.tooltips;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ItemZoomPresentationTest {
    @Test
    void categoriesIncludeForgeToolsAndWearables() {
        assertEquals(ItemZoomPresentation.Category.BLOCK,
                category(new ItemStack(new ItemBlock(new TestBlock()))));
        for (int armorType = 0; armorType <= 3; armorType++) {
            assertEquals(ItemZoomPresentation.Category.EQUIPMENT, category(armor(armorType)));
        }
        // 1.7.10 has no shield; every other held tool class the preview recognises is present.
        for (Item item : new Item[]{new ItemHoe(Item.ToolMaterial.IRON), new ItemShears(),
                new ItemFishingRod(), new ItemBow()}) {
            assertEquals(ItemZoomPresentation.Category.TOOL, category(new ItemStack(item)));
        }
        Item hammer = new Item() {
            @Override public Set<String> getToolClasses(ItemStack stack) {
                return Collections.singleton("hammer");
            }
        };
        assertEquals(ItemZoomPresentation.Category.TOOL, category(new ItemStack(hammer)));
        assertEquals(ItemZoomPresentation.Category.OTHER, category(new ItemStack(new Item())));
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

    /**
     * The whitelist and blacklist rules match the item's "modid:name" registry string, which the
     * unit-test JVM cannot populate: 1.7.10's registries are Forge-controlled and only registration
     * through GameData fills them. Only the scope rule stays observable here.
     */
    @Test
    void categoryModesKeepTheConfiguredScopeRule() {
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.zoomOverlayScope = "tools";
            TooltipConfig.zoomOverlayWhitelist = Collections.emptyList();
            TooltipConfig.zoomOverlayBlacklist = Collections.emptyList();
            assertFalse(ItemZoomOverlay.allowed(new ItemStack(new ItemBlock(new TestBlock()))));
            assertTrue(ItemZoomOverlay.allowed(new ItemStack(new ItemHoe(Item.ToolMaterial.IRON))));
            assertTrue(ItemZoomOverlay.allowed(armor(1)));
            TooltipConfig.zoomOverlayScope = "all";
            assertTrue(ItemZoomOverlay.allowed(new ItemStack(new ItemBlock(new TestBlock()))));
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
            ItemStack chest = armor(1);
            NfrTooltipApi.ArmorPreviewRequest request = ItemZoomRenderer.armorRequest(chest, 96);
            assertEquals(NfrTooltipApi.ArmorPreviewModel.PLAYER, request.model());
            assertEquals(NfrTooltipApi.ArmorSlot.CHEST, request.slot());
            assertEquals(0, request.rotationSpeed());
            assertTrue(request.effects().isEmpty());
            TooltipConfig.zoomOverlayArmorModel = "armor_stand";
            assertEquals(NfrTooltipApi.ArmorPreviewModel.ARMOR_STAND,
                    ItemZoomRenderer.armorRequest(chest, 96).model());
            assertEquals(NfrTooltipApi.ArmorPreviewModel.ARMOR_STAND,
                    ItemZoomRenderer.armorRequest(armor(0), 96).model());
        } finally { original.restore(); }
    }

    @Test
    void headsAndOtherWearablesRespectFollowAndExplicitZoomModels() {
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.zoomOverlayArmorMode = "single_piece";
            for (int armorType = 0; armorType <= 3; armorType++) {
                ItemStack stack = armor(armorType);
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
        ItemStack wornChest = armor(1);
        ItemStack helmet = armor(0);
        ItemStack hovered = new ItemStack(new ItemArmor(ItemArmor.ArmorMaterial.DIAMOND, 0, 1));
        List<ItemStack> outfit = PreviewEquipment.outfit(hovered, NfrTooltipApi.ArmorSlot.CHEST,
                slot -> slot == NfrTooltipApi.ArmorSlot.HEAD ? helmet
                        : slot == NfrTooltipApi.ArmorSlot.CHEST ? wornChest : null);
        // 1.7.10 records an absent layer as null instead of ItemStack.EMPTY.
        List<ItemStack> worn = new ArrayList<>();
        for (ItemStack stack : outfit) if (stack != null) worn.add(stack);
        assertTrue(worn.stream().anyMatch(stack -> stack.getItem() == hovered.getItem()));
        assertFalse(worn.stream().anyMatch(stack -> stack.getItem() == wornChest.getItem()));
        ItemStack copiedHelmet = worn.stream().filter(stack -> stack.getItem() == helmet.getItem())
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

    private static ItemStack armor(int armorType) {
        return new ItemStack(new ItemArmor(ItemArmor.ArmorMaterial.IRON, 0, armorType));
    }

    /** 1.7.10's Block constructor is protected, so the item-block case needs its own subclass. */
    private static final class TestBlock extends Block {
        private TestBlock() {
            super(Material.rock);
        }
    }
}
