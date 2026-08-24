package neofontrender.addons.controller.sdl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AxisNormalizerTest {
    @Test
    void suppressesAndRescalesDeadzone() {
        assertEquals(0.0F, AxisNormalizer.applyDeadzone(0.15F, 0.15F));
        assertEquals(0.5F, AxisNormalizer.applyDeadzone(0.575F, 0.15F), 1.0E-6F);
        assertEquals(-0.5F, AxisNormalizer.applyDeadzone(-0.575F, 0.15F), 1.0E-6F);
    }

    @Test
    void mapsBothShortExtremesToTheNormalizedRange() {
        assertEquals(-1.0F, AxisNormalizer.normalize(Short.MIN_VALUE, 0.0F));
        assertEquals(1.0F, AxisNormalizer.normalize(Short.MAX_VALUE, 0.0F));
    }
}
