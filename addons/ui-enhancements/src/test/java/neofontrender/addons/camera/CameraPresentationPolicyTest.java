package neofontrender.addons.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

class CameraPresentationPolicyTest {
    @Test
    void shoulderAndDroneAreNotClosedByTheFreeLookPerspectiveGuard() {
        assertTrue(CameraPresentationPolicy.freeLookPerspectiveValid(false, 1, 0));
        assertTrue(CameraPresentationPolicy.freeLookPerspectiveValid(false, 0, 1));
    }

    @Test
    void activeFreeLookClosesWhenVanillaPerspectiveChanges() {
        assertTrue(CameraPresentationPolicy.freeLookPerspectiveValid(true, 1, 1));
        assertFalse(CameraPresentationPolicy.freeLookPerspectiveValid(true, 0, 1));
    }

    @Test
    void builtInDetachedModesUseThirdPersonExceptExplicitShoulderFirstPerson() {
        assertEquals(1, CameraPresentationPolicy.builtInPerspective(false, false));
        assertEquals(1, CameraPresentationPolicy.builtInPerspective(true, false));
        assertEquals(0, CameraPresentationPolicy.builtInPerspective(true, true));
    }

    @Test
    void everyDetachedLookModeUsesItsAuthoritativeQuaternion() {
        assertTrue(CameraPresentationPolicy.usesQuaternionView(false, true, false));
        assertTrue(CameraPresentationPolicy.usesQuaternionView(false, false, true));
        assertFalse(CameraPresentationPolicy.usesQuaternionView(true, true, false));
        assertFalse(CameraPresentationPolicy.usesQuaternionView(false, false, false));
    }

    @Test
    void authoritativeFlightFrameUsesQuaternionWithoutChangingShoulderOrVanillaViews() {
        assertTrue(CameraPresentationPolicy.usesQuaternionView(false, false, false, true));
        assertFalse(CameraPresentationPolicy.usesQuaternionView(true, false, false, true));
        assertFalse(CameraPresentationPolicy.usesQuaternionView(false, false, false, false));
    }

    @Test
    void onlyAnActiveAuthoritativeNonProxyFrameOwnsTheNativeCameraAnchor() {
        assertTrue(CameraPresentationPolicy.usesNativeCameraAnchor(true, true, false, false),
                "an API provider that owns the view without requesting a proxy uses this path");
        assertFalse(CameraPresentationPolicy.usesNativeCameraAnchor(false, true, false, false));
        assertFalse(CameraPresentationPolicy.usesNativeCameraAnchor(true, false, false, false));
        assertFalse(CameraPresentationPolicy.usesNativeCameraAnchor(true, true, true, false),
                "shoulder/look/drone keep their existing camera presentation");
        assertFalse(CameraPresentationPolicy.usesNativeCameraAnchor(true, true, false, true),
                "proxy-backed presentation keeps its existing native adapter transform");
    }

    @Test
    void flightTrackingOwnsViewBeforeItsFirstFrameSample() {
        assertTrue(CameraPresentationPolicy.flightCameraViewActive(true, true));
        assertFalse(CameraPresentationPolicy.flightCameraViewActive(false, true));
        assertFalse(CameraPresentationPolicy.flightCameraViewActive(true, false));
    }

    @Test
    void vanillaFrontPreRotationExistsOnlyInFrontPerspective() {
        assertFalse(CameraPresentationPolicy.nativeFrontRotationApplied(0));
        assertFalse(CameraPresentationPolicy.nativeFrontRotationApplied(1));
        assertTrue(CameraPresentationPolicy.nativeFrontRotationApplied(2));
    }

    @Test
    void quaternionViewLeavesVanillaThirdPersonDistanceUntouched() {
        assertTrue(CameraPresentationPolicy.usesQuaternionView(false, false, false, true));
        assertEquals(3.25D,
                CameraPresentationPolicy.vanillaThirdPersonDistance(3.25D, false));
    }

    @Test
    void vanillaThirdPersonDistanceIsOnlySuppressedForAuthoritativeCameraDisplacement() {
        assertEquals(4.0D,
                CameraPresentationPolicy.vanillaThirdPersonDistance(4.0D, false));
        assertEquals(2.75D,
                CameraPresentationPolicy.vanillaThirdPersonDistance(2.75D, false));
        assertEquals(0.0D,
                CameraPresentationPolicy.vanillaThirdPersonDistance(4.0D, true));
        assertEquals(0.0D,
                CameraPresentationPolicy.vanillaThirdPersonDistance(-3.5D, true));
    }

    @Test
    void f5CycleAdvancesEveryVanillaAndBuiltInModeInOrder() {
        List<String> modes = Arrays.asList("first", "third", "shoulder", "free", "drone", "front");
        String active = "first";
        for (int expected = 1; expected < modes.size(); expected++) {
            int next = CameraPresentationPolicy.nextIndex(modes, active);
            assertEquals(expected, next);
            active = modes.get(next);
        }
        assertEquals(0, CameraPresentationPolicy.nextIndex(modes, active));
    }

    @Test
    void staleModeStateRestartsAtFirstConfiguredMode() {
        List<String> modes = Arrays.asList("first", "third", "front");
        assertEquals(0, CameraPresentationPolicy.nextIndex(modes, "removed_custom_mode"));
        assertEquals(0, CameraPresentationPolicy.currentIndex(modes, "removed_custom_mode"));
    }
}
