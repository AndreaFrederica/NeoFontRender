package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShoulderCameraRigTest {
    @Test
    void appliesShoulderOffsetInQuaternionLocalSpace() {
        CameraVector anchor = new CameraVector(10.0D, 64.0D, 10.0D);
        CameraVector identity = ShoulderCameraRig.desired(anchor, CameraAttitude.IDENTITY);
        assertEquals(9.25D, identity.x, 1.0E-9D);
        assertEquals(64.0D, identity.y, 1.0E-9D);
        assertEquals(7.0D, identity.z, 1.0E-9D);

        CameraAttitude yawRight = CameraAttitude.axisAngle(new CameraVector(0.0D, 1.0D, 0.0D),
                Math.PI * 0.5D);
        CameraVector rotated = ShoulderCameraRig.desired(anchor, yawRight);
        assertEquals(7.0D, rotated.x, 1.0E-9D);
        assertEquals(64.0D, rotated.y, 1.0E-9D);
        assertEquals(10.75D, rotated.z, 1.0E-9D);
    }

    @Test
    void preservesOriginalTickTransitionAcrossFrameRates() {
        assertEquals(0.25D, ShoulderCameraRig.transitionBlend(0.25D, 0.05D), 1.0E-9D);
        double atSixtyFps = ShoulderCameraRig.transitionBlend(0.25D, 1.0D / 60.0D);
        double threeFrames = 1.0D - Math.pow(1.0D - atSixtyFps, 3.0D);
        assertEquals(0.25D, threeFrames, 1.0E-9D);
    }

    @Test
    void interpolatesTickOwnedShoulderPositionWithoutFrameTimeState() {
        CameraVector previous = new CameraVector(-0.75D, 0.0D, -3.0D);
        CameraVector current = new CameraVector(0.75D, 0.5D, -2.0D);

        CameraVector start = ShoulderCameraRig.interpolate(previous, current, 0.0D);
        CameraVector middle = ShoulderCameraRig.interpolate(previous, current, 0.5D);
        CameraVector end = ShoulderCameraRig.interpolate(previous, current, 1.0D);

        assertEquals(-0.75D, start.x, 1.0E-9D);
        assertEquals(0.0D, middle.x, 1.0E-9D);
        assertEquals(0.25D, middle.y, 1.0E-9D);
        assertEquals(-2.5D, middle.z, 1.0E-9D);
        assertEquals(0.75D, end.x, 1.0E-9D);
    }
}
