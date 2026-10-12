package neofontrender.addons.zoom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoomTransitionTest {
    @Test
    void easesIntoAndOutOfZoom() {
        ZoomTransition transition = new ZoomTransition();
        // Start at 0
        assertEquals(0.0F, transition.update(false, 0L, 200), 0.0001F);
        assertEquals(0.0F, transition.update(true, 0L, 200), 0.0001F);

        // Zoom in: value should increase over time
        float prev = 0.0F;
        for (int i = 1; i <= 10; i++) {
            long nanos = (long) i * 40_000_000L;
            float val = transition.update(true, nanos, 200);
            assertTrue(val >= prev, "should increase: " + val + " < " + prev);
            prev = val;
        }
        // Should reach ~1.0
        assertEquals(1.0F, prev, 0.1F);

        // Zoom out: value should decrease
        float prevOut = 1.0F;
        for (int i = 1; i <= 10; i++) {
            long nanos = 400_000_000L + (long) i * 40_000_000L;
            float val = transition.update(false, nanos, 200);
            assertTrue(val <= prevOut, "should decrease: " + val + " > " + prevOut);
            prevOut = val;
        }
        // Should reach ~0.0
        assertEquals(0.0F, prevOut, 0.1F);
    }

    @Test
    void reversesWithoutJumping() {
        ZoomTransition transition = new ZoomTransition();
        transition.update(false, 0L, 200);
        float beforeReverse = transition.update(true, 40_000_000L, 200);
        float atReverse = transition.update(false, 40_000_000L, 200);
        float afterReverse = transition.update(false, 60_000_000L, 200);

        assertEquals(beforeReverse, atReverse, 0.0001F);
        assertTrue(afterReverse < atReverse);
    }

    @Test
    void disabledTransitionSnapsToTarget() {
        ZoomTransition transition = new ZoomTransition();
        assertEquals(1.0F, transition.update(true, 0L, 0), 0.0001F);
        assertEquals(0.0F, transition.update(false, 1L, 0), 0.0001F);
    }
}
