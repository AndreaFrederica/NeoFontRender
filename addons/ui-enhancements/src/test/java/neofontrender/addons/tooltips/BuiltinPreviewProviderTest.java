package neofontrender.addons.tooltips;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
