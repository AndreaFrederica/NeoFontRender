package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraFrame;
import neofontrender.addons.api.camera.CameraVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void cursorTargetOnlyChangesPlayerFacingNotTheAuthoritativeCameraRay() {
        CameraVector body = new CameraVector(0.0D, 64.0D, 0.0D);
        CameraFrame frame = new CameraFrame(2L, 0.0F, CameraAttitude.IDENTITY,
                CameraAttitude.IDENTITY, body,
                new CameraVector(-1.0D, 65.0D, -3.0D), false);
        CameraPickingService.RayPlan camera = new CameraPickingService.RayPlan(
                frame.position(), new CameraVector(0.0D, 0.0D, 1.0D), 10.0D);
        CameraVector target = new CameraVector(-1.0D, 65.0D, 7.0D);

        CameraVector playerDirection = CameraPickingService.cursorAimDirection(
                frame, camera, target);

        CameraVector expected = target.subtract(body).normalize();
        assertEquals(expected.x, playerDirection.x, 1.0E-9D);
        assertEquals(expected.y, playerDirection.y, 1.0E-9D);
        assertEquals(expected.z, playerDirection.z, 1.0E-9D);
        assertEquals(frame.position().x, camera.origin.x, 1.0E-9D);
        assertEquals(frame.position().y, camera.origin.y, 1.0E-9D);
        assertEquals(frame.position().z, camera.origin.z, 1.0E-9D);
        assertEquals(10.0D, camera.distance, 1.0E-9D);
    }

    @Test
    void cursorMissUsesIndependentFarAimDistanceWithoutExtendingInteractionRay() {
        CameraPickingService.RayPlan interaction = new CameraPickingService.RayPlan(
                new CameraVector(-0.75D, 65.0D, -3.0D),
                new CameraVector(0.2D, 0.0D, 1.0D).normalize(), 5.0D);

        CameraVector target = CameraPickingService.cursorAimTarget(
                interaction, null, 400.0D);

        CameraVector expected = interaction.origin.add(interaction.direction.scale(400.0D));
        assertEquals(expected.x, target.x, 1.0E-9D);
        assertEquals(expected.y, target.y, 1.0E-9D);
        assertEquals(expected.z, target.z, 1.0E-9D);
        assertEquals(5.0D, interaction.distance, 1.0E-9D);
    }

    @Test
    void cursorReachableHitRemainsTheAimTarget() {
        CameraPickingService.RayPlan interaction = new CameraPickingService.RayPlan(
                new CameraVector(0.0D, 0.0D, 0.0D),
                new CameraVector(0.0D, 0.0D, 1.0D), 5.0D);
        CameraVector hit = new CameraVector(1.0D, 2.0D, 3.0D);

        CameraVector target = CameraPickingService.cursorAimTarget(
                interaction, hit, 400.0D);

        assertEquals(hit.x, target.x, 1.0E-9D);
        assertEquals(hit.y, target.y, 1.0E-9D);
        assertEquals(hit.z, target.z, 1.0E-9D);
    }

    @Test
    void freeLookControlOwnershipSelectsTheMatchingInteractionRay() {
        assertTrue(CameraPickingService.usesPlayerInteractionRay(
                false, false, true, true));
        assertFalse(CameraPickingService.usesPlayerInteractionRay(
                false, false, true, false));
        assertTrue(CameraPickingService.usesPlayerInteractionRay(
                true, true, false, false));
        assertFalse(CameraPickingService.usesPlayerInteractionRay(
                true, false, false, false));
    }

    @Test
    void freeLookOrbitRayIsAnchoredBackAtPlayerReach() {
        CameraFrame frame = new CameraFrame(3L, 0.0F, CameraAttitude.IDENTITY,
                CameraAttitude.IDENTITY, new CameraVector(0.0D, 65.0D, 0.0D),
                new CameraVector(0.0D, 65.0D, -4.0D), false);

        CameraPickingService.RayPlan ray = CameraPickingService.shoulderRay(frame, 5.0D, true);

        assertEquals(0.0D, ray.origin.x, 1.0E-9D);
        assertEquals(65.0D, ray.origin.y, 1.0E-9D);
        assertEquals(0.0D, ray.origin.z, 1.0E-9D);
        assertEquals(0.0D, ray.direction.x, 1.0E-9D);
        assertEquals(0.0D, ray.direction.y, 1.0E-9D);
        assertEquals(1.0D, ray.direction.z, 1.0E-9D);
        assertEquals(5.0D, ray.distance, 1.0E-9D);
    }
}
