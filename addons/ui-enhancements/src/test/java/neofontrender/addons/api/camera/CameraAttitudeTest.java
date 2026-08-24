package neofontrender.addons.api.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraAttitudeTest {
    @Test
    void rotatesBasisWithoutEulerRoundTrip() {
        CameraAttitude attitude = CameraAttitude.axisAngle(
                new CameraVector(0.0D, 1.0D, 0.0D), Math.PI * 0.5D);
        CameraVector forward = attitude.forward();
        assertEquals(1.0D, forward.x, 1.0E-9D);
        assertEquals(0.0D, forward.y, 1.0E-9D);
        assertEquals(0.0D, forward.z, 1.0E-9D);
        assertEquals(0.0D, attitude.angularDistance(attitude), 1.0E-12D);
    }

    @Test
    void bodyAndViewRemainIndependentInFrame() {
        CameraAttitude body = CameraAttitude.IDENTITY;
        CameraAttitude view = CameraAttitude.axisAngle(
                new CameraVector(0.0D, 1.0D, 0.0D), 0.25D);
        CameraFrame frame = new CameraFrame(4L, 0.5F, body, view,
                new CameraVector(1.0D, 2.0D, 3.0D),
                new CameraVector(2.0D, 2.0D, 3.0D), false);
        assertEquals(0.0D, frame.bodyAttitude().angularDistance(body), 1.0E-12D);
        assertTrue(frame.viewAttitude().angularDistance(body) > 0.1D);
        assertEquals(1.0D, frame.bodyBasis().forward().z, 1.0E-9D);
    }
}
