package neofontrender.addons.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControllerCursorMotionTest {
    @Test void startsAtBaseAcceleratesAndStopsAtMaximum() {
        ControllerCursorMotion motion = new ControllerCursorMotion();
        assertEquals(120.0D, motion.update(1.0F, 0.0F, 0.0D,
                120.0D, 360.0D, 600.0D), 1.0E-9D);
        assertEquals(150.0D, motion.update(1.0F, 0.0F, 0.05D,
                120.0D, 360.0D, 600.0D), 1.0E-9D);
        for (int frame = 0; frame < 20; frame++) {
            motion.update(1.0F, 0.0F, 0.05D, 120.0D, 360.0D, 600.0D);
        }
        assertEquals(360.0D, motion.update(1.0F, 0.0F, 0.05D,
                120.0D, 360.0D, 600.0D), 1.0E-9D);
    }

    @Test void neutralInputResetsTheNextMovementToBaseSpeed() {
        ControllerCursorMotion motion = new ControllerCursorMotion();
        motion.update(1.0F, 0.0F, 0.05D, 100.0D, 300.0D, 1_000.0D);
        motion.update(1.0F, 0.0F, 0.05D, 100.0D, 300.0D, 1_000.0D);
        assertEquals(100.0D, motion.update(0.0F, 0.0F, 0.05D,
                100.0D, 300.0D, 1_000.0D), 1.0E-9D);
        assertEquals(100.0D, motion.update(1.0F, 0.0F, 0.05D,
                100.0D, 300.0D, 1_000.0D), 1.0E-9D);
    }

    @Test void frameTimeIsClampedAndMaximumCannotFallBelowBase() {
        ControllerCursorMotion motion = new ControllerCursorMotion();
        assertEquals(200.0D, motion.update(1.0F, 0.0F, 0.0D,
                200.0D, 100.0D, 2_000.0D), 1.0E-9D);
        assertEquals(200.0D, motion.update(1.0F, 0.0F, 10.0D,
                200.0D, 100.0D, 2_000.0D), 1.0E-9D);
    }
}
