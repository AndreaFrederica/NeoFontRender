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

    @Test
    void continuousStepRemainsSmoothWhenTargetChanges() {
        float moving = SmoothScrollController.continuousStep(0.0F, 40.0F, 16_000_000L, 200);
        float retargeted = SmoothScrollController.continuousStep(moving, 80.0F, 16_000_000L, 200);

        assertTrue(retargeted > moving);
        assertTrue(retargeted < 80.0F);
    }
}
