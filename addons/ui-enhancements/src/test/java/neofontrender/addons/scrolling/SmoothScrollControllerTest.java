package neofontrender.addons.scrolling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothScrollControllerTest {
    @Test
    void clampsTargetsAndResetsWhenContentDoesNotScroll() {
        SmoothScrollController controller = new SmoothScrollController();
        controller.sync(20.0F);
        controller.scrollBy(200.0F, 100.0F, 20.0F);
        assertEquals(100.0F, controller.getTarget());
        assertEquals(0.0F, controller.update(50.0F, 0.0F));
        assertEquals(0.0F, controller.getTarget());
    }

    @Test
    void movesTowardTheTargetWithoutOvershooting() {
        SmoothScrollController controller = new SmoothScrollController();
        controller.sync(0.0F);
        controller.scrollBy(40.0F, 100.0F, 0.0F);
        float value = controller.update(0.0F, 100.0F);
        assertTrue(value >= 0.0F && value <= 40.0F);
    }
}
