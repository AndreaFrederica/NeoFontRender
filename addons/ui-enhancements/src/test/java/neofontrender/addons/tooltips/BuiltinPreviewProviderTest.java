package neofontrender.addons.tooltips;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BuiltinPreviewProviderTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        net.minecraft.init.Bootstrap.register();
    }

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
            java.util.List<ItemStack> wearables = new java.util.ArrayList<>();
            for (int type = 0; type <= 5; type++) wearables.add(new ItemStack(Items.SKULL, 1, type));
            wearables.add(new ItemStack(Blocks.PUMPKIN));
            wearables.add(new ItemStack(Items.ELYTRA));
            for (String model : new String[]{"armor_stand", "player"}) {
                TooltipConfig.armorPreviewModel = model;
                for (ItemStack stack : wearables) {
                    NfrTooltipApi.TooltipDocument document = BuiltinPreviewProvider.INSTANCE.create(
                            stack, Collections.singletonList("Wearable")).orElseThrow();
                    NfrTooltipApi.ArmorPreviewRequest request = (NfrTooltipApi.ArmorPreviewRequest)
                            ((NfrTooltipApi.PreviewNode) document.nodes.get(0)).request();
                    assertEquals("player".equals(model) ? NfrTooltipApi.ArmorPreviewModel.PLAYER
                            : NfrTooltipApi.ArmorPreviewModel.ARMOR_STAND, request.model());
                    assertEquals(stack.getItem() == Items.ELYTRA ? EntityEquipmentSlot.CHEST
                            : EntityEquipmentSlot.HEAD, request.slot());
                    assertEquals(stack.getMetadata(), request.stack().getMetadata());
                }
            }
        } finally { original.restore(); }
    }

    @Test
    void whitelistExplicitlyEnablesAnOrdinaryItem() {
        Item item = new Item().setRegistryName(new ResourceLocation("test", "ordinary"));
        boolean oldEnabled = TooltipConfig.itemPreviewEnabled;
        String oldScope = TooltipConfig.itemPreviewScope;
        java.util.List<String> oldWhitelist = TooltipConfig.previewWhitelist;
        java.util.List<String> oldBlacklist = TooltipConfig.previewBlacklist;
        try {
            TooltipConfig.itemPreviewEnabled = true;
            TooltipConfig.itemPreviewScope = "tools";
            TooltipConfig.previewWhitelist = Collections.singletonList("test:ordinary");
            TooltipConfig.previewBlacklist = Collections.emptyList();

            assertTrue(BuiltinPreviewProvider.INSTANCE.create(
                    new ItemStack(item), Collections.singletonList("Ordinary")).isPresent());
        } finally {
            TooltipConfig.itemPreviewEnabled = oldEnabled;
            TooltipConfig.itemPreviewScope = oldScope;
            TooltipConfig.previewWhitelist = oldWhitelist;
            TooltipConfig.previewBlacklist = oldBlacklist;
        }
    }
}
