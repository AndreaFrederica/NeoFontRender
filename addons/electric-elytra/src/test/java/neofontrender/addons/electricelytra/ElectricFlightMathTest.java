package neofontrender.addons.electricelytra;

import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightVector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ElectricFlightMathTest {
    private static final double EPSILON = 1.0E-8D;

    @Test
    void poweredAccelerationIsParallelToLookVector() {
        ElectricFlightMath.FlightStep idle = ElectricFlightMath.step(0.0D, 0.0D, 0.0D,
                0.6D, 0.0D, 0.8D, false, 1.0D);
        ElectricFlightMath.FlightStep powered = ElectricFlightMath.step(0.0D, 0.0D, 0.0D,
                0.6D, 0.0D, 0.8D, true, 1.0D);
        double deltaX = powered.velocityX - idle.velocityX;
        double deltaY = powered.velocityY - idle.velocityY;
        double deltaZ = powered.velocityZ - idle.velocityZ;
        assertEquals(0.0D, deltaY, EPSILON);
        assertEquals(0.6D / 0.8D, deltaX / deltaZ, EPSILON);
    }

    @Test
    void throttleScalesThrustLinearly() {
        ElectricFlightMath.FlightStep idle = ElectricFlightMath.step(0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 1.0D, false, 0.0D);
        ElectricFlightMath.FlightStep fivePercent = ElectricFlightMath.step(0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 1.0D, true, 0.05D);
        ElectricFlightMath.FlightStep full = ElectricFlightMath.step(0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 1.0D, true, 1.0D);
        assertEquals((full.velocityZ - idle.velocityZ) * 0.05D,
                fivePercent.velocityZ - idle.velocityZ, EPSILON);
    }

    @Test
    void fireworkAccelerationUsesTheSameContinuousThrustIntegrator() {
        ElectricFlightMath.FlightStep idle = ElectricFlightMath.step(
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D,
                0.0D, false, 0.0D, 0, 0.0D, 0.0D);
        ElectricFlightMath.FlightStep boosted = ElectricFlightMath.step(
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D,
                0.0D, false, 0.0D, 0, 0.0D, 24.0D);
        assertEquals(24.0D / 20.0D,
                boosted.velocityZ - idle.velocityZ, EPSILON);
        assertEquals(idle.velocityY, boosted.velocityY, EPSILON);
    }

    @Test
    void aerodynamicDragFollowsVelocitySquared() {
        ElectricFlightMath.FlightStep slow = ElectricFlightMath.step(20.0D, 0.0D, 0.0D,
                1.0D, 0.0D, 0.0D, false, 0.0D);
        ElectricFlightMath.FlightStep fast = ElectricFlightMath.step(40.0D, 0.0D, 0.0D,
                1.0D, 0.0D, 0.0D, false, 0.0D);
        assertEquals(4.0D, fast.dragAcceleration / slow.dragAcceleration, EPSILON);
    }

    @Test
    void positiveAngleOfAttackProducesPositiveLift() {
        double tenDegrees = Math.toRadians(10.0D);
        ElectricFlightMath.FlightStep result = ElectricFlightMath.step(25.0D, 0.0D, 0.0D,
                Math.cos(tenDegrees), Math.sin(tenDegrees), 0.0D, false, 0.0D);
        assertTrue(result.liftCoefficient > ElectricElytraConfig.liftCoefficientAtZeroAngle);
        assertTrue(result.liftAcceleration > 0.0D);
    }

    @Test
    void deepStallAddsSeparatedFlowDrag() {
        double normalAngle = Math.toRadians(10.0D);
        double stalledAngle = Math.toRadians(60.0D);
        ElectricFlightMath.FlightStep normal = ElectricFlightMath.step(25.0D, 0.0D, 0.0D,
                Math.cos(normalAngle), Math.sin(normalAngle), 0.0D, false, 0.0D);
        ElectricFlightMath.FlightStep stalled = ElectricFlightMath.step(25.0D, 0.0D, 0.0D,
                Math.cos(stalledAngle), Math.sin(stalledAngle), 0.0D, false, 0.0D);
        assertTrue(stalled.dragCoefficient > normal.dragCoefficient);
    }

    @Test
    void hardLimitClampsExtremeVelocity() {
        ElectricFlightMath.FlightStep result = ElectricFlightMath.step(500.0D, 0.0D, 0.0D,
                1.0D, 0.0D, 0.0D, true, 1.0D);
        double speed = Math.sqrt(result.velocityX * result.velocityX
                + result.velocityY * result.velocityY + result.velocityZ * result.velocityZ);
        assertTrue(speed <= ElectricElytraConfig.hardSpeedLimitBlocksPerSecond + EPSILON);
    }

    @Test
    void sideslipForceOpposesLateralVelocity() {
        ElectricFlightMath.FlightStep result = ElectricFlightMath.step(8.0D, 0.0D, 24.0D,
                0.0D, 0.0D, 1.0D, false, 0.0D);
        assertTrue(Math.abs(result.sideslipAngleRadians) > 0.0D);
        assertTrue(8.0D * result.sideVectorX + 24.0D * result.sideVectorZ < 0.0D);
    }

    @Test
    void passiveYawMomentAlwaysOpposesSideslip() {
        assertTrue(ElectricFlightMath.directionalYawMomentCoefficient(0.2D, 0.0D) > 0.0D);
        assertTrue(ElectricFlightMath.directionalYawMomentCoefficient(-0.2D, 0.0D) < 0.0D);
    }

    @Test
    void sideslipRetainsDirectionPastNinetyDegrees() {
        double angle = Math.toRadians(120.0D);
        assertEquals(angle, ElectricFlightMath.sideslipAngle(
                Math.cos(angle), Math.sin(angle)), EPSILON);
        assertTrue(ElectricFlightMath.directionalYawMomentCoefficient(angle, 0.0D) > 0.0D);
    }

    @Test
    void yawRateDampingAlwaysOpposesRotation() {
        assertTrue(ElectricFlightMath.yawAngularAcceleration(
                80.0D, 0.0D, 0.0D, 0.5D, false, 0.0D) < 0.0D);
        assertTrue(ElectricFlightMath.yawAngularAcceleration(
                80.0D, 0.0D, 0.0D, -0.5D, false, 0.0D) > 0.0D);
    }

    @Test
    void largeSideslipRecoversWithoutReverseLock() {
        double beta = Math.toRadians(145.0D);
        double yawRate = 0.0D;
        for (int tick = 0; tick < 600; tick++) {
            double acceleration = ElectricFlightMath.yawAngularAcceleration(
                    70.0D, beta, 0.0D, yawRate, false, 0.0D);
            yawRate = clampYawRate(yawRate + acceleration / 20.0D);
            // Positive yaw turns toward positive (aircraft-right) beta, reducing the error.
            beta = wrapRadians(beta - yawRate / 20.0D);
        }
        assertTrue(Math.abs(beta) < Math.toRadians(2.0D));
        assertTrue(Math.abs(yawRate) < Math.toRadians(2.0D));
    }

    @Test
    void sasYawHoldSettlesInsteadOfEnteringLimitCycle() {
        double headingError = Math.toRadians(80.0D);
        double yawRate = 0.0D;
        double maximumLateRate = 0.0D;
        for (int tick = 0; tick < 600; tick++) {
            double acceleration = ElectricFlightMath.yawAngularAcceleration(
                    90.0D, headingError, 0.0D, yawRate,
                    true, headingError);
            yawRate = clampYawRate(yawRate + acceleration / 20.0D);
            headingError = wrapRadians(headingError - yawRate / 20.0D);
            if (tick >= 500) maximumLateRate = Math.max(maximumLateRate, Math.abs(yawRate));
        }
        assertTrue(Math.abs(headingError) < Math.toRadians(0.5D));
        assertTrue(maximumLateRate < Math.toRadians(0.5D));
    }

    @Test
    void landingFlapsIncreaseLiftAndDrag() {
        ElectricFlightMath.FlightStep clean = ElectricFlightMath.step(25.0D, 0.0D, 0.0D,
                1.0D, 0.0D, 0.0D, false, 0.0D, 0);
        ElectricFlightMath.FlightStep landing = ElectricFlightMath.step(25.0D, 0.0D, 0.0D,
                1.0D, 0.0D, 0.0D, false, 0.0D, 2);
        assertTrue(landing.liftCoefficient > clean.liftCoefficient);
        assertTrue(landing.dragCoefficient > clean.dragCoefficient);
    }

    @Test
    void bankRotatesLiftAwayFromWorldUp() {
        ElectricFlightMath.FlightStep level = ElectricFlightMath.step(0.0D, 0.0D, 25.0D,
                0.0D, 0.0D, 1.0D, 0.0D, false, 0.0D, 0);
        ElectricFlightMath.FlightStep banked = ElectricFlightMath.step(0.0D, 0.0D, 25.0D,
                0.0D, 0.0D, 1.0D, Math.PI / 2.0D, false, 0.0D, 0);
        assertTrue(level.liftVectorY > 0.0D);
        assertEquals(0.0D, banked.liftVectorY, EPSILON);
        // Positive UIE bank is right-wing-down; facing south, aircraft-right is -X.
        assertTrue(banked.liftVectorX < 0.0D);
    }

    @Test
    void oppositeBanksProduceOppositeSignedLiftWithoutReversal() {
        ElectricFlightMath.FlightStep rightBank = ElectricFlightMath.step(
                0.0D, 0.0D, 25.0D, 0.0D, 0.0D, 1.0D,
                Math.toRadians(82.0D), false, 0.0D, 0);
        ElectricFlightMath.FlightStep leftBank = ElectricFlightMath.step(
                0.0D, 0.0D, 25.0D, 0.0D, 0.0D, 1.0D,
                Math.toRadians(-82.0D), false, 0.0D, 0);
        assertTrue(rightBank.liftVectorX < 0.0D);
        assertTrue(leftBank.liftVectorX > 0.0D);
    }

    @Test
    void invertedNegativeAngleOfAttackProducesUpwardLift() {
        double pitch = Math.toRadians(10.0D);
        ElectricFlightMath.FlightStep inverted = ElectricFlightMath.step(
                25.0D, 0.0D, 0.0D, Math.cos(pitch), Math.sin(pitch), 0.0D,
                Math.PI, false, 0.0D, 0);
        assertTrue(inverted.angleOfAttackRadians < 0.0D);
        assertTrue(inverted.liftCoefficient < 0.0D);
        assertTrue(inverted.liftVectorY > 0.0D);
    }

    @Test
    void aerodynamicRudderGeneratesSideForce() {
        ElectricFlightMath.FlightStep neutral = ElectricFlightMath.step(
                0.0D, 0.0D, 25.0D, 0.0D, 0.0D, 1.0D,
                0.0D, false, 0.0D, 0, 0.0D);
        ElectricFlightMath.FlightStep rudder = ElectricFlightMath.step(
                0.0D, 0.0D, 25.0D, 0.0D, 0.0D, 1.0D,
                0.0D, false, 0.0D, 0, 1.0D);
        assertEquals(0.0D, neutral.sideAcceleration, EPSILON);
        // Positive UIE yaw/rudder is aircraft-right, which is -X while facing south.
        assertTrue(rudder.sideVectorX < 0.0D);
    }

    @Test
    void flapDetentsLowerCalculatedStallSpeed() {
        double clean = ElectricFlightMath.stallSpeedBlocksPerSecond(0);
        double takeoff = ElectricFlightMath.stallSpeedBlocksPerSecond(1);
        double landing = ElectricFlightMath.stallSpeedBlocksPerSecond(2);
        assertTrue(takeoff < clean);
        assertTrue(landing < takeoff);
    }

    @Test
    void aerodynamicForcesRemainContinuousAcrossVerticalPole() {
        ElectricFlightMath.FlightStep previous = null;
        for (double pitch : new double[]{-89.0D, -90.0D, -91.0D}) {
            FlightAttitude attitude = FlightAttitude.fromMinecraftDegrees(pitch, 23.0D, 37.0D);
            FlightVector forward = attitude.forward();
            FlightVector up = attitude.up();
            double velocityX = forward.x * 25.0D - up.x * 2.0D;
            double velocityY = forward.y * 25.0D - up.y * 2.0D;
            double velocityZ = forward.z * 25.0D - up.z * 2.0D;
            ElectricFlightMath.FlightStep current = ElectricFlightMath.step(
                    velocityX, velocityY, velocityZ, attitude,
                    false, 0.0D, 0, 0.0D, 0.0D);
            assertTrue(Double.isFinite(current.liftVectorX));
            assertTrue(Double.isFinite(current.liftVectorY));
            assertTrue(Double.isFinite(current.liftVectorZ));
            if (previous != null) {
                double delta = Math.sqrt(square(current.liftVectorX - previous.liftVectorX)
                        + square(current.liftVectorY - previous.liftVectorY)
                        + square(current.liftVectorZ - previous.liftVectorZ));
                assertTrue(delta < 1.0D, "lift jumped at vertical pole: " + delta);
            }
            previous = current;
        }
    }

    private static double square(double value) { return value * value; }

    private static double clampYawRate(double value) {
        double limit = Math.toRadians(ElectricElytraConfig.bodyAxisTurnRateDegreesPerSecond);
        return Math.max(-limit, Math.min(limit, value));
    }

    private static double wrapRadians(double value) {
        while (value > Math.PI) value -= Math.PI * 2.0D;
        while (value < -Math.PI) value += Math.PI * 2.0D;
        return value;
    }
}
