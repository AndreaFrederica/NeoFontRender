package neofontrender.addons.api.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CameraValueTypesTest {
    @Test
    void zeroLengthPublicRaysUseStableForwardDirection() {
        CameraVector zero = new CameraVector(0.0D, 0.0D, 0.0D);
        CameraRay ray = new CameraRay(null, zero);
        CameraPickingRequest request = new CameraPickingRequest(null, zero, 5.0D,
                null, false, false);

        assertEquals(1.0D, ray.direction().z, 0.0D);
        assertEquals(1.0D, request.direction().z, 0.0D);
        assertEquals(1.0D, ray.direction().length(), 1.0E-12D);
        assertEquals(1.0D, request.direction().length(), 1.0E-12D);
    }

    @Test
    void missHitStillExposesFinitePosition() {
        CameraHit hit = new CameraHit(null, null);
        assertNotNull(hit.position());
        assertEquals(0.0D, hit.position().lengthSquared(), 0.0D);
    }
}
