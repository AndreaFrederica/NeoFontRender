package neofontrender.addons.api.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class FlightBodyPoseTest {
    @Test
    void api9ConstructorDescriptorRemainsAvailable() throws Exception {
        assertNotNull(FlightBodyPose.class.getConstructor(FlightAttitude.class));
    }

    @Test
    void oldConstructorKeepsAircraftForwardZBasis() {
        FlightAttitude attitude = FlightAttitude.fromMinecraftDegrees(-23.0D, 71.0D, 37.0D);

        FlightBodyPose pose = new FlightBodyPose(attitude);

        assertSame(attitude, pose.attitude);
        assertEquals(FlightBodyPose.Basis.LEGACY_AIRCRAFT_FORWARD_Z, pose.basis);
        assertNull(pose.headWorldAttitude);
    }

    @Test
    void modelUpYFactoryDeclaresItsBasisWithoutChangingTheQuaternion() {
        FlightAttitude attitude = FlightAttitude.fromMinecraftDegrees(19.0D, -54.0D, 81.0D);

        FlightBodyPose pose = FlightBodyPose.modelUpY(attitude);

        assertSame(attitude, pose.attitude);
        assertEquals(FlightBodyPose.Basis.MODEL_UP_Y, pose.basis);
        assertNull(pose.headWorldAttitude);
    }

    @Test
    void modelUpYFactoryCanCarryTheHeadWorldOrientationFromTheSameSample() {
        FlightAttitude body = FlightAttitude.fromMinecraftDegrees(-25.0D, 49.0D, 11.0D);
        FlightAttitude head = FlightAttitude.fromMinecraftDegrees(18.0D, 64.0D, 0.0D);

        FlightBodyPose pose = FlightBodyPose.modelUpY(body, head);

        assertSame(body, pose.attitude);
        assertSame(head, pose.headWorldAttitude);
        assertEquals(FlightBodyPose.Basis.MODEL_UP_Y, pose.basis);
    }
}
