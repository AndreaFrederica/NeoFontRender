package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DroneMotionControllerTest {
    @Test
    void integratesLocalMotionWithoutAnyPlayerState() {
        DroneMotionController controller = new DroneMotionController(
                new CameraVector(0.0D, 0.0D, 0.0D), CameraAttitude.IDENTITY);
        controller.move(0.0D, 0.0D, 1.0D, 0.05D);
        assertTrue(controller.position().z > 0.0D);

        CameraAttitude before = controller.attitude();
        controller.look(100, 0, false);
        assertTrue(controller.attitude().angularDistance(before) > 0.01D);
    }

    @Test
    void mouseLookDoesNotCreateRollAndDedicatedAxisDoes() {
        double previousRollSpeed = DroneCameraConfig.rollSpeedDegrees;
        try {
            DroneCameraConfig.rollSpeedDegrees = 180.0D;
            DroneMotionController controller = new DroneMotionController(
                    new CameraVector(0.0D, 0.0D, 0.0D), CameraAttitude.IDENTITY);
            controller.look(50, 25, false);
            assertEquals(0.0D, controller.attitude().toMinecraftEuler(0.0D, 0.0D, 0.0D)
                    .rollDegrees, 1.0E-6D);
            CameraAttitude beforeRoll = controller.attitude();
            controller.roll(1.0D, 0.05D);
            assertTrue(controller.attitude().angularDistance(beforeRoll) > 0.1D);
        } finally {
            DroneCameraConfig.rollSpeedDegrees = previousRollSpeed;
        }
    }

    @Test
    void configuredSpeedAndResponseControlTranslation() {
        double previousSpeed = DroneCameraConfig.speed;
        double previousResponse = DroneCameraConfig.translationResponse;
        try {
            DroneCameraConfig.speed = 20.0D;
            DroneCameraConfig.translationResponse = 0.0D;
            DroneMotionController controller = new DroneMotionController(
                    new CameraVector(0.0D, 0.0D, 0.0D), CameraAttitude.IDENTITY);
            controller.move(0.0D, 0.0D, 1.0D, 0.05D);
            assertEquals(1.0D, controller.position().z, 1.0E-9D);
        } finally {
            DroneCameraConfig.speed = previousSpeed;
            DroneCameraConfig.translationResponse = previousResponse;
        }
    }
}
