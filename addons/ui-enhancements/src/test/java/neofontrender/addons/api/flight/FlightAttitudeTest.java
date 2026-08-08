package neofontrender.addons.api.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightAttitudeTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void localFrameRemainsOrthonormalAtBothVerticalPoles() {
        for (double pitch : new double[]{-90.0D, 90.0D}) {
            FlightAttitude attitude = FlightAttitude.fromMinecraftDegrees(pitch, 73.0D, 41.0D);
            FlightVector forward = attitude.forward();
            FlightVector right = attitude.right();
            FlightVector up = attitude.up();
            assertEquals(1.0D, forward.lengthSquared(), EPSILON);
            assertEquals(1.0D, right.lengthSquared(), EPSILON);
            assertEquals(1.0D, up.lengthSquared(), EPSILON);
            assertEquals(0.0D, forward.dot(right), EPSILON);
            assertEquals(0.0D, forward.dot(up), EPSILON);
            assertEquals(0.0D, right.dot(up), EPSILON);
        }
    }

    @Test
    void cameraEulerBranchContinuesThroughUpwardPole() {
        FlightEulerAngles before = FlightAttitude.fromMinecraftDegrees(-89.0D, 0.0D, 0.0D)
                .toMinecraftEuler(-88.0D, 0.0D, 0.0D);
        FlightEulerAngles pole = FlightAttitude.fromMinecraftDegrees(-90.0D, 0.0D, 0.0D)
                .toMinecraftEuler(before.pitchDegrees, before.yawDegrees, before.rollDegrees);
        FlightEulerAngles after = FlightAttitude.fromMinecraftDegrees(-91.0D, 0.0D, 0.0D)
                .toMinecraftEuler(pole.pitchDegrees, pole.yawDegrees, pole.rollDegrees);
        assertEquals(-89.0F, before.pitchDegrees, 0.001F);
        assertEquals(-90.0F, pole.pitchDegrees, 0.001F);
        assertEquals(-91.0F, after.pitchDegrees, 0.001F);
        assertEquals(0.0F, after.yawDegrees, 0.001F);
        assertEquals(0.0F, after.rollDegrees, 0.001F);
    }

    @Test
    void fourPitchRotationsCompleteLoopWithoutChangingWingAxes() {
        FlightAttitude attitude = FlightAttitude.IDENTITY;
        for (int i = 0; i < 4; i++) attitude = attitude.rotateLocal(
                1.0D, 0.0D, 0.0D, Math.PI / 2.0D);
        assertTrue(attitude.angularDistance(FlightAttitude.IDENTITY) < EPSILON);
        assertEquals(-1.0D, attitude.right().x, EPSILON);
        assertEquals(1.0D, attitude.up().y, EPSILON);
        assertEquals(1.0D, attitude.forward().z, EPSILON);
    }

    @Test
    void basisRoundTripPreservesInvertedAttitude() {
        FlightAttitude source = FlightAttitude.fromMinecraftDegrees(32.0D, -121.0D, 180.0D);
        FlightAttitude rebuilt = FlightAttitude.fromBasis(
                source.right(), source.up(), source.forward());
        assertTrue(source.angularDistance(rebuilt) < EPSILON);
    }

    @Test
    void mixedAttitudesRoundTripThroughCameraBoundary() {
        double referencePitch = 0.0D, referenceYaw = 0.0D, referenceRoll = 0.0D;
        for (double pitch = -270.0D; pitch <= 270.0D; pitch += 15.0D) {
            double yaw = pitch * 0.73D + 41.0D;
            double roll = pitch * -0.47D + 27.0D;
            FlightAttitude source = FlightAttitude.fromMinecraftDegrees(pitch, yaw, roll);
            FlightEulerAngles camera = source.toMinecraftEuler(
                    referencePitch, referenceYaw, referenceRoll);
            FlightAttitude rebuilt = FlightAttitude.fromMinecraftDegrees(
                    camera.pitchDegrees, camera.yawDegrees, camera.rollDegrees);
            assertTrue(source.angularDistance(rebuilt) < 1.0E-5D,
                    "round trip failed at pitch " + pitch);
            assertTrue(Math.abs(camera.pitchDegrees - referencePitch) <= 180.01D);
            referencePitch = camera.pitchDegrees;
            referenceYaw = camera.yawDegrees;
            referenceRoll = camera.rollDegrees;
        }
    }
}
