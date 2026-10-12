package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraAttitude;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeLookControllerTest {
    @Test
    void matchesVanillaSensitivityAndOmnilookTurnScale() {
        double previousResponse = FreeLookConfig.mouseResponse;
        double previousInertia = FreeLookConfig.orientationResponse;
        try {
            FreeLookConfig.mouseResponse = 1.0D;
            FreeLookConfig.orientationResponse = 0.0D;
            FreeLookController controller = new FreeLookController();
            controller.look(10, 0, false, 0.5D);
            double degrees = Math.toDegrees(controller.resolve(CameraAttitude.IDENTITY)
                    .angularDistance(CameraAttitude.IDENTITY));
            assertEquals(1.5D, degrees, 1.0E-6D);
            assertEquals(1.0D, FreeLookController.vanillaMouseScale(0.5D), 1.0E-9D);
        } finally {
            FreeLookConfig.mouseResponse = previousResponse;
            FreeLookConfig.orientationResponse = previousInertia;
        }
    }

    @Test
    void viewAttitudeDoesNotFollowLaterBodyRotation() {
        double previousInertia = FreeLookConfig.orientationResponse;
        try {
            FreeLookConfig.orientationResponse = 0.0D;
            CameraAttitude initial = CameraAttitude.fromMinecraftDegrees(20.0D, 35.0D, 15.0D);
            FreeLookController controller = new FreeLookController(initial);
            CameraAttitude changedBody = CameraAttitude.fromMinecraftDegrees(-45.0D, 170.0D, -80.0D);

            assertTrue(controller.resolve(changedBody).angularDistance(initial) < 1.0E-9D);
        } finally {
            FreeLookConfig.orientationResponse = previousInertia;
        }
    }

    @Test
    void dedicatedAxisRollsAroundTheLocalViewAxis() {
        double previousRollSpeed = FreeLookConfig.rollSpeedDegrees;
        double previousInertia = FreeLookConfig.orientationResponse;
        try {
            FreeLookConfig.rollSpeedDegrees = 180.0D;
            FreeLookConfig.orientationResponse = 0.0D;
            FreeLookController controller = new FreeLookController();

            controller.roll(1.0D, 0.05D);

            CameraAttitude attitude = controller.resolve(CameraAttitude.IDENTITY);
            assertEquals(9.0D, Math.toDegrees(attitude.angularDistance(CameraAttitude.IDENTITY)),
                    1.0E-6D);
            assertEquals(0.0D, attitude.forward().subtract(
                    CameraAttitude.IDENTITY.forward()).length(), 1.0E-9D);
        } finally {
            FreeLookConfig.rollSpeedDegrees = previousRollSpeed;
            FreeLookConfig.orientationResponse = previousInertia;
        }
    }
}
