package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CameraPresentationTransformTest {
    @Test
    void identityViewConvertsShoulderOffsetToOrientCameraTranslation() {
        CameraVector translation = CameraPresentationTransform.translation(
                CameraAttitude.IDENTITY, new CameraVector(10.0D, 20.0D, 30.0D),
                new CameraVector(9.25D, 20.5D, 27.0D));

        assertEquals(-0.75D, translation.x, 1.0E-9D);
        assertEquals(-0.5D, translation.y, 1.0E-9D);
        assertEquals(-3.0D, translation.z, 1.0E-9D);
    }

    @Test
    void rotatedWorldOffsetReturnsTheSameCameraLocalTranslation() {
        CameraAttitude view = CameraAttitude.fromMinecraftDegrees(20.0D, 75.0D, 15.0D);
        CameraVector anchor = new CameraVector(-4.0D, 70.0D, 12.0D);
        CameraVector localOffset = new CameraVector(0.8D, -0.25D, -4.0D);
        CameraVector camera = anchor.add(view.rotate(localOffset));

        CameraVector translation = CameraPresentationTransform.translation(view, anchor, camera);

        assertEquals(localOffset.x, translation.x, 1.0E-9D);
        assertEquals(-localOffset.y, translation.y, 1.0E-9D);
        assertEquals(localOffset.z, translation.z, 1.0E-9D);
    }
}
