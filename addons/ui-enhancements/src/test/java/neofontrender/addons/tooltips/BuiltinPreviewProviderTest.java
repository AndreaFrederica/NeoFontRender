package neofontrender.addons.tooltips;

import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinPreviewProviderTest {
    @Test
    void recognizesForgeDeclaredToolClasses() {
        Item forgeTool = new Item() {
            @Override
            public Set<String> getToolClasses(ItemStack stack) {
                return Collections.singleton("hammer");
            }
        };

        assertTrue(BuiltinPreviewProvider.isToolOrWeapon(new ItemStack(forgeTool)));
        assertFalse(BuiltinPreviewProvider.isToolOrWeapon(new ItemStack(new Item())));
    }

    @Test
    void headsAndOtherWearablesUseTheConfiguredTooltipModel() {
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.armorPreviewEnabled = true;
            TooltipConfig.armorPreviewMode = "single_piece";
            TooltipConfig.previewWhitelist = Collections.emptyList();
            TooltipConfig.previewBlacklist = Collections.emptyList();
            // 1.7.10 cannot populate the vanilla registries inside the unit-test JVM, so the four
            // wearable slots are covered with directly built armor pieces, helmet first.
            NfrTooltipApi.ArmorSlot[] slots = {NfrTooltipApi.ArmorSlot.HEAD,
                    NfrTooltipApi.ArmorSlot.CHEST, NfrTooltipApi.ArmorSlot.LEGS,
                    NfrTooltipApi.ArmorSlot.FEET};
            for (String model : new String[]{"armor_stand", "player"}) {
                TooltipConfig.armorPreviewModel = model;
                NfrTooltipApi.ArmorPreviewModel expected =
                        "player".equals(model) ? NfrTooltipApi.ArmorPreviewModel.PLAYER
                                : NfrTooltipApi.ArmorPreviewModel.ARMOR_STAND;
                for (int armorType = 0; armorType < slots.length; armorType++) {
                    NfrTooltipApi.ArmorPreviewRequest request = requestFor(armor(armorType));
                    assertEquals(expected, request.model());
                    assertEquals(slots[armorType], request.slot());
                }
            }
        } finally {
            original.restore();
        }
    }

    @Test
    void ordinaryItemsFollowTheItemPreviewScope() {
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.itemPreviewEnabled = true;
            TooltipConfig.itemPreviewScope = "tools";
            TooltipConfig.previewWhitelist = Collections.emptyList();
            TooltipConfig.previewBlacklist = Collections.emptyList();
            assertFalse(create(new ItemStack(new Item())).isPresent());

            TooltipConfig.itemPreviewScope = "all";
            assertTrue(create(new ItemStack(new Item())).isPresent());
        } finally {
            original.restore();
        }
    }

    private static Optional<NfrTooltipApi.TooltipDocument> create(ItemStack stack) {
        return BuiltinPreviewProvider.INSTANCE.create(stack, Collections.singletonList("Preview"));
    }

    private static NfrTooltipApi.ArmorPreviewRequest requestFor(ItemStack stack) {
        NfrTooltipApi.TooltipDocument document = create(stack).orElseThrow();
        NfrTooltipApi.ArmorPreviewRequest request = (NfrTooltipApi.ArmorPreviewRequest)
                ((NfrTooltipApi.PreviewNode) document.nodes.get(0)).request();
        assertEquals(stack.getItemDamage(), request.stack().getItemDamage());
        return request;
    }

    private static ItemStack armor(int armorType) {
        return new ItemStack(new ItemArmor(ItemArmor.ArmorMaterial.IRON, 0, armorType), 1, armorType);
    }
}
