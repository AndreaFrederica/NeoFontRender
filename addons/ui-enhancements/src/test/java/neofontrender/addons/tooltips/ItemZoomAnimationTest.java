package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemZoomAnimationTest {
    private final ItemZoomAnimation animation = new ItemZoomAnimation();

    private float at(String item, int millis, String switching) {
        return animation.progress(item, millis * 1_000_000L, true, 200, switching);
    }

    @Test
    void rapidSwitchesContinueOneTimelineIncludingReturningToPreviousItem() {
        assertEquals(0, at("A", 0, "continue"));
        assertEquals(0.5F, at("B", 100, "continue"), 0.0001F);
        assertTrue(at("A", 150, "continue") > 0.5F);
        assertEquals(1, at("C", 200, "continue"));
        assertEquals(1, at("D", 210, "continue"));
    }

    @Test
    void restartNeverResumesAnOldItemsCachedProgress() {
        at("A", 0, "restart");
        assertEquals(0.5F, at("A", 100, "restart"));
        assertEquals(0, at("B", 110, "restart"));
        assertEquals(0, at("A", 120, "restart"));
        assertEquals(0.5F, at("A", 220, "restart"));
    }

    @Test
    void instantStillAnimatesFirstAppearanceButCompletesOnSwitch() {
        assertEquals(0, at("A", 0, "instant"));
        assertEquals(0.5F, at("A", 100, "instant"));
        assertEquals(1, at("B", 110, "instant"));
        assertEquals(1, at("A", 120, "instant"));
    }

    @Test
    void briefGapsPreserveProgressButNewHoverSessionsAnimateAgain() {
        at("A", 0, "continue");
        assertEquals(0.5F, at("B", 100, "continue"));
        assertEquals(0, at("B", 700, "continue"));
        assertEquals(0.5F, at("C", 800, "continue"));
        animation.reset();
        assertEquals(0, at("C", 810, "continue"));
    }

    @Test
    void disabledAndZeroDurationNeverProduceAnInvisibleFirstFrame() {
        assertEquals(1, animation.progress("A", 0, false, 200, "restart"));
        assertEquals(1, animation.progress("B", 1, true, 0, "restart"));
        assertEquals(0, animation.progress("C", 2, true, 200, "restart"));
    }

    @Test
    void aSlowFirstMeasurementDoesNotRestartAContinuouslyVisiblePreview() {
        at("A", 0, "continue");
        animation.rendered(800_000_000L);
        assertEquals(1, at("A", 810, "continue"));
    }
}
