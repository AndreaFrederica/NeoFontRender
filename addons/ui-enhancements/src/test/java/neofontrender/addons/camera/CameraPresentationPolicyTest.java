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
    void authoritativeFlightTrackingUsesTheSampledQuaternionWithoutASeparateRig() {
        assertTrue(CameraPresentationPolicy.usesQuaternionView(false, false, false, true));
        assertFalse(CameraPresentationPolicy.usesQuaternionView(true, false, false, true));
        assertFalse(CameraPresentationPolicy.usesQuaternionView(false, false, false, false));
    }

    @Test
    void playerAnchoredThirdPersonPresentationOwnsFlightAndRigDistance() {
        assertFalse(CameraPresentationPolicy.ownsPlayerAnchoredThirdPersonPresentation(
                true, false, false, false, 1));
        assertFalse(CameraPresentationPolicy.ownsPlayerAnchoredThirdPersonPresentation(
                true, false, false, false, 2));
        assertTrue(CameraPresentationPolicy.ownsPlayerAnchoredThirdPersonPresentation(
                true, false, false, true, 1));
        assertTrue(CameraPresentationPolicy.ownsPlayerAnchoredThirdPersonPresentation(
                true, false, false, true, 2));
        assertTrue(CameraPresentationPolicy.ownsPlayerAnchoredThirdPersonPresentation(
                true, true, false, false, 1));
        assertFalse(CameraPresentationPolicy.ownsPlayerAnchoredThirdPersonPresentation(
                true, true, false, true, 0));
        assertFalse(CameraPresentationPolicy.ownsPlayerAnchoredThirdPersonPresentation(
                false, false, false, true, 1));
    }

    @Test
    void onlyFlightQuaternionFrontViewIsNormalizedForVanillaOrientation() {
        assertEquals(1, CameraPresentationPolicy.vanillaOrientationPerspective(1, true));
        assertEquals(1, CameraPresentationPolicy.vanillaOrientationPerspective(2, true));
        assertEquals(2, CameraPresentationPolicy.vanillaOrientationPerspective(2, false));
    }

    @Test
    void playerAnchoredThirdPersonDisablesVanillaDistanceWithoutChangingDetachedBehavior() {
        assertFalse(CameraPresentationPolicy.suppressesVanillaThirdPersonDistance(false, false));
        assertTrue(CameraPresentationPolicy.suppressesVanillaThirdPersonDistance(true, false));
        assertTrue(CameraPresentationPolicy.suppressesVanillaThirdPersonDistance(false, true));
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
