package neofontrender.addons.loading;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Arc3DMaterialSpinnerRendererTest {
    @Test
    void morphCycleCarriesTailAdvanceIntoNextLoop() {
        long duration = 1_333_000_000L;
        float before = Arc3DMaterialSpinnerRenderer.continuousStartDegrees(
                duration - 1L, 0L, 286.0F - 42.0F);
        float after = Arc3DMaterialSpinnerRenderer.continuousStartDegrees(
                duration, 1L, 0.0F);
        float wrappedDelta = Math.abs(((after - before + 540.0F) % 360.0F) - 180.0F);
        assertTrue(wrappedDelta < 0.01F, "loop boundary jumped by " + wrappedDelta + " degrees");
    }
}
