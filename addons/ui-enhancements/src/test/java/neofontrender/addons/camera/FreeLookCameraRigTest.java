package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraVector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FreeLookCameraRigTest {
    @AfterEach
    void clearOffset() {
        FreeLookCameraRig.resetMoveOffset();
        FreeLookCameraRig.beginTick();
    }

    @Test
    void interpolatesCameraMovementAcrossTheRenderTick() {
        FreeLookCameraRig.resetMoveOffset();
        FreeLookCameraRig.beginTick();
        FreeLookCameraRig.adjustMoveOffset(1.0D, -2.0D, 3.0D);

        assertVector(0.0D, 0.0D, 0.0D,
                FreeLookCameraRig.interpolatedMoveOffset(0.0F));
        assertVector(0.25D, -0.5D, 0.75D,
                FreeLookCameraRig.interpolatedMoveOffset(0.25F));
        assertVector(1.0D, -2.0D, 3.0D,
                FreeLookCameraRig.interpolatedMoveOffset(1.0F));
    }

    @Test
    void beginsTheNextTickFromTheLastRenderedTarget() {
        FreeLookCameraRig.resetMoveOffset();
        FreeLookCameraRig.beginTick();
        FreeLookCameraRig.adjustMoveOffset(1.0D, 0.0D, 0.0D);
        FreeLookCameraRig.beginTick();
        FreeLookCameraRig.adjustMoveOffset(1.0D, 0.0D, 0.0D);

        assertVector(1.0D, 0.0D, 0.0D,
                FreeLookCameraRig.interpolatedMoveOffset(0.0F));
        assertVector(1.5D, 0.0D, 0.0D,
                FreeLookCameraRig.interpolatedMoveOffset(0.5F));
        assertVector(2.0D, 0.0D, 0.0D,
                FreeLookCameraRig.interpolatedMoveOffset(1.0F));
    }

    private static void assertVector(double x, double y, double z, CameraVector actual) {
        assertEquals(x, actual.x, 1.0E-9D);
        assertEquals(y, actual.y, 1.0E-9D);
        assertEquals(z, actual.z, 1.0E-9D);
    }
}
