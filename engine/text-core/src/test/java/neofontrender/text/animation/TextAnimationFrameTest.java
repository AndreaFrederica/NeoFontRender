package neofontrender.text.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextAnimationFrameTest {
    @Test
    void nestedDrawBranchesShareTheOuterFrame() {
        try (TextAnimationFrame.Scope ignored = TextAnimationFrame.open(41L)) {
            assertEquals(41L, TextAnimationFrame.current());
            try (TextAnimationFrame.Scope nested = TextAnimationFrame.open(99L)) {
                assertEquals(41L, TextAnimationFrame.current());
            }
            assertEquals(41L, TextAnimationFrame.current());
        }
    }
}
