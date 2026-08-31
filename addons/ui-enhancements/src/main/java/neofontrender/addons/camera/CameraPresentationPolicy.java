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

    /** Detached look rigs render from their frame instead of vanilla player yaw/pitch. */
    static boolean usesQuaternionView(boolean shoulderActive, boolean lookCameraActive,
                                      boolean droneActive) {
        return usesQuaternionView(shoulderActive, lookCameraActive, droneActive, false);
    }

    /**
     * A FlightCameraTracking attitude is already represented by the sampled CameraFrame.
     * Present that same frame directly instead of mixing its roll with tick-owned Euler angles.
     */
    static boolean usesQuaternionView(boolean shoulderActive, boolean lookCameraActive,
                                      boolean droneActive, boolean flightAuthoritativeFrame) {
        return !shoulderActive && (lookCameraActive || droneActive || flightAuthoritativeFrame);
    }

    /**
     * A player-anchored rig owns third-person orbit translation. This includes an authoritative
     * Flight frame even when no built-in camera mode is active.
     */
    static boolean ownsPlayerAnchoredThirdPersonPresentation(boolean playerViewEntity,
                                                              boolean shoulderActive,
                                                              boolean lookCameraActive,
                                                              boolean flightAuthoritativeFrame,
                                                              int thirdPersonView) {
        return playerViewEntity && thirdPersonView > 0
                && (shoulderActive || lookCameraActive || flightAuthoritativeFrame);
    }

    static boolean suppressesVanillaThirdPersonDistance(boolean detachedPresentation,
                                                        boolean playerAnchoredPresentation) {
        return detachedPresentation || playerAnchoredPresentation;
    }

    /** A Flight frame already contains its local front-view turn; vanilla must not apply it again. */
    static int vanillaOrientationPerspective(int thirdPersonView,
                                             boolean flightQuaternionPresentation) {
        return flightQuaternionPresentation && thirdPersonView == 2 ? 1 : thirdPersonView;
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
