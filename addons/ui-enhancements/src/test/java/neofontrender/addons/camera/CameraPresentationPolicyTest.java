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
