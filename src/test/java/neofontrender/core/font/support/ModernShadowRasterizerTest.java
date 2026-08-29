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
        assertEquals(0x80102030, result.pixels[3]);
    }

    @Test
    void combinesConfiguredColorAlphaWithShadowOpacityExactlyOnce() {
        ModernShadowRasterizer.Result result = ModernShadowRasterizer.compose(
                new int[]{0xFFFFFFFF}, 1, 1, 1.0F,
                1.0F, 1.0F, 0.0F, 0x80204060, 0.5F, true);
        assertEquals(64, result.pixels[3] >>> 24);
    }

    @Test
    void gaussianBlurProducesAContinuousImpulseFalloff() {
        ModernShadowRasterizer.Result result = ModernShadowRasterizer.shadow(
                new int[]{0xFFFFFFFF}, 1, 1, 1.0F,
                0.0F, 0.0F, 1.0F, 0xFFFFFFFF, 1.0F, false);
        int center = result.pixels[result.originY * result.width + result.originX] >>> 24;
        int adjacent = result.pixels[result.originY * result.width + result.originX + 1] >>> 24;
        int diagonal = result.pixels[(result.originY + 1) * result.width + result.originX + 1]
                >>> 24;
        int outer = result.pixels[result.originY * result.width + result.originX + 2] >>> 24;
        assertTrue(center > adjacent, "blur center should retain the greatest coverage");
        assertTrue(adjacent > diagonal, "axis coverage should exceed diagonal coverage");
        assertTrue(diagonal > outer, "coverage should decay smoothly from the impulse");
    }

    @Test
    void fractionalBlurRadiusDoesNotCollapseToAnUnblurredPixel() {
        ModernShadowRasterizer.Result result = ModernShadowRasterizer.shadow(
                new int[]{0xFFFFFFFF}, 1, 1, 1.0F,
                0.0F, 0.0F, 0.5F, 0xFFFFFFFF, 1.0F, false);
        int adjacent = result.pixels[result.originY * result.width + result.originX + 1] >>> 24;
        assertTrue(adjacent > 0, "fractional blur should produce neighboring coverage");
    }
}
