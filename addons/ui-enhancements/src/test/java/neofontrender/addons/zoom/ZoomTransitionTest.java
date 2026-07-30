package neofontrender.addons.zoom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoomTransitionTest {
    @Test
    void easesIntoAndOutOfZoom() {
        ZoomTransition transition = new ZoomTransition();
        assertEquals(0.0F, transition.update(false, 0L, 200), 0.0001F);
        assertEquals(0.0F, transition.update(true, 0L, 200), 0.0001F);
        assertEquals(0.5F, transition.update(true, 100_000_000L, 200), 0.0001F);
        assertEquals(1.0F, transition.update(true, 200_000_000L, 200), 0.0001F);
        assertEquals(0.5F, transition.update(false, 300_000_000L, 200), 0.0001F);
        assertEquals(0.0F, transition.update(false, 400_000_000L, 200), 0.0001F);
    }

    @Test
    void reversesWithoutJumping() {
        ZoomTransition transition = new ZoomTransition();
        transition.update(false, 0L, 200);
        float beforeReverse = transition.update(true, 80_000_000L, 200);
        float atReverse = transition.update(false, 80_000_000L, 200);
        float afterReverse = transition.update(false, 100_000_000L, 200);

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
