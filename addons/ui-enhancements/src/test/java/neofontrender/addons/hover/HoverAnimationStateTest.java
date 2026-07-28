package neofontrender.addons.hover;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoverAnimationStateTest {
    @Test
    void transitionReversesFromItsCurrentProgress() {
        HoverAnimationState state = new HoverAnimationState();
        state.update(true, 100, 200, 1_000_000_000L);
        state.update(true, 100, 200, 1_050_000_000L);
        assertEquals(0.5F, state.progress(), 0.001F);

        state.update(false, 100, 200, 1_100_000_000L);
        assertEquals(0.25F, state.progress(), 0.001F);
    }

    @Test
    void longFrameIsCappedAndInstantDurationSnaps() {
        HoverAnimationState state = new HoverAnimationState();
        state.update(true, 1000, 1000, 1_000_000_000L);
        state.update(true, 1000, 1000, 4_000_000_000L);
        assertTrue(state.progress() <= 0.101F);

        state.update(true, 0, 0, 4_010_000_000L);
        assertEquals(1.0F, state.progress(), 0.0F);
        state.update(false, 0, 0, 4_020_000_000L);
        assertEquals(0.0F, state.progress(), 0.0F);
    }

    @Test
    void instantDurationSnapsOnTheFirstUpdate() {
        HoverAnimationState state = new HoverAnimationState();

        state.update(true, 0, 0, 1_000_000_000L);
        assertEquals(1.0F, state.progress(), 0.0F);
    }
}
