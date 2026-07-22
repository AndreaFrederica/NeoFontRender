package neofontrender.addons.hud;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import neofontrender.addons.hud.api.HudBarValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FoodBarValuesTest {
    @Test
    void consumesInjectedFoodAdapterWithoutMinecraftInternals() {
        FoodStatsAccess access = new FixedFoodStatsAccess(2.0F, 4.0F, new FoodPreview(6, 7.2F));

        HudBarValue value = FoodBarValues.create(
                8.0F, 3.0F, null, null, access,
                1, 2, 3, 4, "8/20");

        assertEquals(8.0F, value.current);
        assertEquals(3.0F, value.secondary);
        assertEquals(6.0F, value.preview);
        assertEquals(10.0F, value.depletion);
        assertEquals("8/20", value.text);
    }

    @Test
    void rejectsBrokenAdapterExhaustionScale() {
        FoodStatsAccess access = new FixedFoodStatsAccess(1.0F, 0.0F, FoodPreview.NONE);

        assertThrows(IllegalStateException.class,
                () -> FoodBarValues.create(8.0F, 3.0F, null, null, access, 1, 2, 3, 4, ""));
    }

    private static final class FixedFoodStatsAccess implements FoodStatsAccess {
        private final float exhaustion;
        private final float maximumExhaustion;
        private final FoodPreview preview;

        private FixedFoodStatsAccess(float exhaustion, float maximumExhaustion, FoodPreview preview) {
            this.exhaustion = exhaustion;
            this.maximumExhaustion = maximumExhaustion;
            this.preview = preview;
        }

        @Override public float exhaustion(EntityPlayer player) { return exhaustion; }
        @Override public float maximumExhaustion(EntityPlayer player) { return maximumExhaustion; }
        @Override public FoodPreview preview(ItemStack stack, EntityPlayer player) { return preview; }
    }
}
