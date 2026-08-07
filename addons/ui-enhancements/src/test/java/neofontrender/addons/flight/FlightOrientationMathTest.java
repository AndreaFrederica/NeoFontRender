package neofontrender.addons.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlightOrientationMathTest {
    private static final float EPSILON = 0.001F;

    @Test
    void forgeCameraYawLooksAlongMinecraftEntityForward() {
        assertEquals(180.0F, FlightOrientationMath.cameraEventYaw(0.0F), 0.0F);
        assertEquals(270.0F, FlightOrientationMath.cameraEventYaw(90.0F), 0.0F);
        assertEquals(90.0F, FlightOrientationMath.cameraEventYaw(-90.0F), 0.0F);
    }

    @Test
    void uprightLocalAxesMatchMinecraftPitchAndYaw() {
        FlightOrientationMath.Orientation pitch = FlightOrientationMath.rotate(
                0.0F, 0.0F, 0.0F, 10.0D, 0.0D, 0.0D);
        FlightOrientationMath.Orientation yaw = FlightOrientationMath.rotate(
                0.0F, 0.0F, 0.0F, 0.0D, 10.0D, 0.0D);

        assertEquals(10.0F, pitch.pitch, EPSILON);
        assertEquals(0.0F, pitch.yaw, EPSILON);
        assertEquals(0.0F, pitch.roll, EPSILON);
        assertEquals(0.0F, yaw.pitch, EPSILON);
        assertEquals(10.0F, yaw.yaw, EPSILON);
    }

    @Test
    void pitchAtNinetyDegreeRollBecomesAHorizontalTurn() {
        FlightOrientationMath.Orientation orientation = FlightOrientationMath.rotate(
                0.0F, 0.0F, 90.0F, 10.0D, 0.0D, 0.0D);

        assertEquals(0.0F, orientation.pitch, EPSILON);
        assertEquals(-10.0F, orientation.yaw, EPSILON);
        assertEquals(90.0F, orientation.roll, EPSILON);
    }

    @Test
    void rollIsContinuousAcrossAnyNumberOfTurns() {
        FlightOrientationMath.Orientation orientation = FlightOrientationMath.rotate(
                15.0F, 25.0F, 350.0F, 0.0D, 0.0D, 30.0D);

        assertEquals(15.0F, orientation.pitch, EPSILON);
        assertEquals(25.0F, orientation.yaw, EPSILON);
        assertEquals(380.0F, orientation.roll, EPSILON);
    }
}
