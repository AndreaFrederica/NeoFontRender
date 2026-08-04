package neofontrender.core.font.backend;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampledShadowTextRenderResultTest {
    @Test void scalesGeometryAndKeepsRequestedCompositeOpacity() {
        RecordingResult shadow = new RecordingResult();
        RecordingResult foreground = new RecordingResult();
        SampledShadowTextRenderResult result = new SampledShadowTextRenderResult(
                shadow, foreground, 1.0F, 2.0F, 0.5F, 0.25F, 4.0F);
        result.draw(10.0F, 20.0F, 0.8F);

        assertEquals(9, shadow.draws.size());
        assertEquals(1, foreground.draws.size());
        assertTrue(result.visualRight() >= 15.0F);
        double reconstructed = 1.0D - Math.pow(
                1.0D - shadow.draws.get(0)[2] / 0.8D, shadow.draws.size());
        assertEquals(0.25D, reconstructed, 0.0001D);
    }

    private static final class RecordingResult implements TextRenderResult {
        private final List<float[]> draws = new ArrayList<>();
        @Override public float advance() { return 10.0F; }
        @Override public void draw(float x, float y, float alpha) {
            draws.add(new float[]{x, y, alpha});
        }
    }
}
