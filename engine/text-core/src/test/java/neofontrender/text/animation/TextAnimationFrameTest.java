package neofontrender.text.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TextAnimationFrameTest {
    @Test
    void nestedDrawBranchesShareTheOuterFrame() {
        try (TextAnimationFrame.Scope ignored = TextAnimationFrame.open(41L)) {
            assertEquals(41L, TextAnimationFrame.current());
            assertEquals(2050L, TextAnimationFrame.currentTimeMillis());
            try (TextAnimationFrame.Scope nested = TextAnimationFrame.open(99L)) {
                assertEquals(41L, TextAnimationFrame.current());
                assertEquals(2050L, TextAnimationFrame.currentTimeMillis());
            }
            assertEquals(41L, TextAnimationFrame.current());
        }
    }

    @Test
    void explicitInstanceOverridesAndThenRestoresOuterIdentity() {
        long outer = TextAnimationFrame.allocateInstanceId();
        long inner = TextAnimationFrame.allocateInstanceId();
        assertNotEquals(outer, inner);
        try (TextAnimationFrame.Scope ignored = TextAnimationFrame.openInstance(outer)) {
            assertEquals(outer, TextAnimationFrame.currentInstanceId());
            try (TextAnimationFrame.Scope nested = TextAnimationFrame.openInstance(inner)) {
                assertEquals(inner, TextAnimationFrame.currentInstanceId());
            }
            assertEquals(outer, TextAnimationFrame.currentInstanceId());
        }
        assertEquals(0L, TextAnimationFrame.currentInstanceId());
    }

    @Test
    void automaticSamePositionOccurrencesReceiveDifferentIds() {
        long firstId;
        try (TextAnimationFrame.Scope first = TextAnimationFrame.openAutomatic(
                "<typewriter>x</typewriter>", 4.0F, 8.0F)) {
            firstId = TextAnimationFrame.currentInstanceId();
        }
        try (TextAnimationFrame.Scope second = TextAnimationFrame.openAutomatic(
                "<typewriter>x</typewriter>", 4.0F, 8.0F)) {
            assertNotEquals(firstId, TextAnimationFrame.currentInstanceId());
        }
    }

    @Test
    void neonSuppressionIsScopedAndRestored() {
        assertEquals(false, TextAnimationFrame.neonOverdrawSuppressed());
        try (TextAnimationFrame.NeonSuppression ignored =
                     TextAnimationFrame.suppressNeonOverdraw()) {
            assertEquals(true, TextAnimationFrame.neonOverdrawSuppressed());
            try (TextAnimationFrame.NeonSuppression nested =
                         TextAnimationFrame.suppressNeonOverdraw()) {
                assertEquals(true, TextAnimationFrame.neonOverdrawSuppressed());
            }
            assertEquals(true, TextAnimationFrame.neonOverdrawSuppressed());
        }
        assertEquals(false, TextAnimationFrame.neonOverdrawSuppressed());
    }
}
