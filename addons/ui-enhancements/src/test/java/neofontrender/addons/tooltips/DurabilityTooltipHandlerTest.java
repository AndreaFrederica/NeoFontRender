package neofontrender.addons.tooltips;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurabilityTooltipHandlerTest {
    @BeforeAll
    static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    void reportsRemainingDurabilityAndClampsBrokenItems() {
        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        int max = stack.getMaxDamage();
        stack.setItemDamage(37);
        assertEquals(max - 37, DurabilityTooltipHandler.remainingDamage(stack));

        stack.setItemDamage(max + 100);
        assertEquals(0, DurabilityTooltipHandler.remainingDamage(stack));
    }

    @Test
    void ignoresItemsWithoutDurability() {
        assertEquals(-1, DurabilityTooltipHandler.remainingDamage(new ItemStack(Items.APPLE)));
        assertEquals(-1, DurabilityTooltipHandler.remainingDamage(ItemStack.EMPTY));
        assertEquals(-1, DurabilityTooltipHandler.remainingDamage(null));
    }
}
