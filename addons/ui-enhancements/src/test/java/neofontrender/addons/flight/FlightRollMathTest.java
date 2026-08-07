package neofontrender.addons.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlightRollMathTest {
    @Test
    void bankingCoordinatesYawWithRollDirection() {
        FlightRollMath.BankingDelta level = FlightRollMath.bankingDelta(0.0F, 0.0F, 1.0D);
        assertEquals(0.0D, level.pitch, 1.0E-9D);
        assertEquals(0.0D, level.yaw, 1.0E-9D);

        FlightRollMath.BankingDelta right = FlightRollMath.bankingDelta(30.0F, 0.0F, 1.0D);
        FlightRollMath.BankingDelta left = FlightRollMath.bankingDelta(-30.0F, 0.0F, 1.0D);
        assertEquals(-26.7949D, right.pitch, 1.0E-4D);
        assertEquals(100.0D, right.yaw, 1.0E-6D);
        assertEquals(right.pitch, left.pitch, 1.0E-9D);
        assertEquals(-right.yaw, left.yaw, 1.0E-9D);
    }

    @Test
    void bankingFadesOutAtVerticalPitch() {
        FlightRollMath.BankingDelta vertical = FlightRollMath.bankingDelta(
                45.0F, 90.0F, 1.0D);
        assertEquals(0.0D, vertical.pitch, 1.0E-9D);
        assertEquals(0.0D, vertical.yaw, 1.0E-9D);
    }

    @Test
    void clampRejectsInvalidAndBoundsFiniteValues() {
        assertEquals(30.0F, FlightRollMath.clamp(80.0F, 30.0F));
        assertEquals(-30.0F, FlightRollMath.clamp(-80.0F, 30.0F));
        assertEquals(0.0F, FlightRollMath.clamp(Float.NaN, 30.0F));
    }

    @Test
    void approachUsesAStableBoundedResponse() {
        assertEquals(2.5F, FlightRollMath.approach(0.0F, 10.0F, 0.25F));
        assertEquals(10.0F, FlightRollMath.approach(0.0F, 10.0F, 5.0F));
    }

    @Test
    void barrelAnimationStartsAndEndsUpright() {
        assertEquals(0.0F, FlightRollMath.barrelAngle(1, 0.0F));
        assertEquals(180.0F, FlightRollMath.barrelAngle(1, 0.5F));
        assertEquals(360.0F, FlightRollMath.barrelAngle(1, 1.0F));
        assertEquals(-180.0F, FlightRollMath.barrelAngle(-1, 0.5F));
    }

    @Test
    void networkOrientationWrapsAcrossCompleteRolls() {
        assertEquals(-170.0F, FlightRollMath.wrapDegrees(190.0F));
        assertEquals(0.0F, FlightRollMath.wrapDegrees(360.0F));
        assertEquals(170.0F, FlightRollMath.wrapDegrees(-190.0F));
    }
}
