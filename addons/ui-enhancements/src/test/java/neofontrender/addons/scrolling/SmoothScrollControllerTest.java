package neofontrender.addons.scrolling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothScrollControllerTest {
    @Test
    void continuousStepMovesMonotonicallyWithoutOvershoot() {
        float first = SmoothScrollController.continuousStep(0.0F, 20.0F, 16_000_000L, 200);
        float second = SmoothScrollController.continuousStep(first, 20.0F, 16_000_000L, 200);

        assertTrue(first > 0.0F && first < 20.0F);
        assertTrue(second > first && second < 20.0F);
    }

    @Test
    void continuousStepDoesNotMoveWithoutElapsedTime() {
        assertEquals(7.0F, SmoothScrollController.continuousStep(7.0F, 20.0F, 0L, 200));
    }
}
