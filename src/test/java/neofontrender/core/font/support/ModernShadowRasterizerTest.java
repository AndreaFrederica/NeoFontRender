package neofontrender.core.font.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernShadowRasterizerTest {
    @Test
    void expandsAndComposesOffsetShadowUnderForeground() {
        ModernShadowRasterizer.Result result = ModernShadowRasterizer.compose(
                new int[]{0xFFFFFFFF}, 1, 1, 1.0F,
                1.0F, 1.0F, 0.0F, 0xFF204060, 0.5F, true);
        assertEquals(2, result.width);
        assertEquals(2, result.height);
        assertEquals(0xFFFFFFFF, result.pixels[0]);
        assertTrue((result.pixels[3] >>> 24) > 0);
    }
}
