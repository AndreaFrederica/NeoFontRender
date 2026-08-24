package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraFrame;
import neofontrender.addons.api.camera.CameraVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CameraPickingServiceTest {
    @Test
    void reproducesShoulderSurfingHeadPlaneAndReachLimit() {
        CameraVector body = new CameraVector(0.0D, 64.0D, 0.0D);
        CameraFrame frame = new CameraFrame(1L, 0.0F, CameraAttitude.IDENTITY,
                CameraAttitude.IDENTITY, body, body.add(new CameraVector(-0.75D, 0.0D, -3.0D)), false);

        CameraPickingService.RayPlan limited = CameraPickingService.shoulderRay(frame, 5.0D, true);
        assertEquals(-0.75D, limited.origin.x, 1.0E-9D);
        assertEquals(64.0D, limited.origin.y, 1.0E-9D);
        assertEquals(0.0D, limited.origin.z, 1.0E-9D);
        assertEquals(Math.sqrt(25.0D - 0.75D * 0.75D), limited.distance, 1.0E-9D);
        assertEquals(1.0D, limited.direction.z, 1.0E-9D);

        CameraPickingService.RayPlan unlimited = CameraPickingService.shoulderRay(frame, 5.0D, false);
        assertEquals(5.0D, unlimited.distance, 1.0E-9D);
    }

    @Test
    void keepsOriginalAdaptiveItemAndPropertyDefaults() {
        assertEquals(7, ShoulderCameraConfig.adaptiveHoldItems.size());
        assertEquals(java.util.Collections.singletonList("minecraft:charged"),
                ShoulderCameraConfig.adaptiveHoldProperties);
        assertEquals(java.util.Arrays.asList("minecraft:pull", "minecraft:throwing"),
                ShoulderCameraConfig.adaptiveUseProperties);
    }
}
