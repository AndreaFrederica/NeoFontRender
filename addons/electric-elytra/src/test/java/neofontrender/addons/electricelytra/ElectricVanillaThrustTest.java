package neofontrender.addons.electricelytra;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ElectricVanillaThrustTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void convertsAccelerationToMinecraftMotionWithoutAerodynamicIntegration() {
        assertEquals(12.0D / 400.0D,
                ElectricVanillaThrust.motionIncrement(12.0D, 1.0D), EPSILON);
        assertEquals(12.0D / 800.0D,
                ElectricVanillaThrust.motionIncrement(12.0D, 0.5D), EPSILON);
    }

    @Test
    void clampsThrottleAndRejectsNegativeAcceleration() {
        assertEquals(12.0D / 400.0D,
                ElectricVanillaThrust.motionIncrement(12.0D, 2.0D), EPSILON);
        assertEquals(0.0D,
                ElectricVanillaThrust.motionIncrement(-12.0D, 1.0D), EPSILON);
    }
}
