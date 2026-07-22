package neofontrender.addons.hud.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HudBarValueTest {
    @Test
    void clampsEveryDynamicLayerToSharedScale() {
        HudBarValue value = new HudBarValue(
                -3.0F, 20.0F, 24.0F, Float.POSITIVE_INFINITY, Float.NaN,
                1, 2, 3, 4, null);

        assertEquals(0.0F, value.current);
        assertEquals(20.0F, value.secondary);
        assertEquals(20.0F, value.preview);
        assertEquals(0.0F, value.depletion);
        assertEquals("", value.text);
    }

    @Test
    void rejectsNonPositiveOrNonFiniteMaximum() {
        assertThrows(IllegalArgumentException.class,
                () -> new HudBarValue(0.0F, 0.0F, 0xFFFFFFFF, ""));
        assertThrows(IllegalArgumentException.class,
                () -> new HudBarValue(0.0F, Float.NaN, 0xFFFFFFFF, ""));
        assertThrows(IllegalArgumentException.class,
                () -> new HudBarValue(0.0F, Float.POSITIVE_INFINITY, 0xFFFFFFFF, ""));
    }
}
