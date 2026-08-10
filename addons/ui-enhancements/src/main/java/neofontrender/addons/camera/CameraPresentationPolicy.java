package neofontrender.addons.camera;

import java.util.List;

/** Pure presentation state rules shared by runtime code and regression tests. */
final class CameraPresentationPolicy {
    private CameraPresentationPolicy() {}

    /** Omnilook exits only when its own active FreeLook perspective is changed externally. */
    static boolean freeLookPerspectiveValid(boolean freeLookActive,
                                            int currentPerspective,
                                            int expectedPerspective) {
        return !freeLookActive || currentPerspective == expectedPerspective;
    }

    static int builtInPerspective(boolean shoulderActive, boolean firstPersonOverride) {
        return shoulderActive && firstPersonOverride ? 0 : 1;
    }

    static int currentIndex(List<?> modes, Object activeMode) {
        if (modes == null || modes.isEmpty()) return -1;
        return Math.max(0, modes.indexOf(activeMode));
    }

    static int nextIndex(List<?> modes, Object activeMode) {
        if (modes == null || modes.isEmpty()) return -1;
        int current = modes.indexOf(activeMode);
        return current < 0 ? 0 : (current + 1) % modes.size();
    }
}
