package neofontrender.addons.loading;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldLoadingGradientTest {
    private static final float ENDPOINT_TOLERANCE = 0.00001F;

    @Test
    void materialCurveHasClampedEndpoints() {
        float start = WorldLoadingRenderer.materialGradientCurve(0.0F);
        float end = WorldLoadingRenderer.materialGradientCurve(1.0F);

        assertEquals(0.0F, start, ENDPOINT_TOLERANCE);
        assertEquals(1.0F, end, ENDPOINT_TOLERANCE);
        assertEquals(start, WorldLoadingRenderer.materialGradientCurve(-1.0F));
        assertEquals(end, WorldLoadingRenderer.materialGradientCurve(2.0F));
    }

    @Test
    void materialCurveIsMonotonic() {
        float previous = WorldLoadingRenderer.materialGradientCurve(0.0F);
        for (int step = 1; step <= 100; step++) {
            float current = WorldLoadingRenderer.materialGradientCurve(step / 100.0F);
            assertTrue(current >= previous,
                    "curve decreased at position " + step / 100.0F);
            previous = current;
        }
    }

    @Test
    void materialCurveMatchesStandardShape() {
        assertEquals(0.237F, WorldLoadingRenderer.materialGradientCurve(0.25F), 0.003F);
        assertEquals(0.776F, WorldLoadingRenderer.materialGradientCurve(0.50F), 0.003F);
        assertEquals(0.959F, WorldLoadingRenderer.materialGradientCurve(0.75F), 0.003F);
    }
}
