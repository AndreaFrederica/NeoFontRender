package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraVector;

import java.util.List;

/** Pure presentation state rules shared by runtime code and regression tests. */
public final class CameraPresentationPolicy {
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

    /** An authoritative frame also owns the final view when Flight supplies its quaternion. */
    static boolean usesQuaternionView(boolean shoulderActive, boolean lookCameraActive,
                                      boolean droneActive, boolean frameAuthoritative) {
        return !shoulderActive && (lookCameraActive || droneActive || frameAuthoritative);
    }

    /** Flight tracking owns the render before a CameraFrame has been sampled this frame. */
    static boolean flightCameraViewActive(boolean trackingAvailable, boolean cameraAllowed) {
        return trackingAvailable && cameraAllowed;
    }

    /**
     * Native orientCamera displacement may be replaced only when a non-proxy UIE frame owns
     * the complete camera presentation. Built-in rigs retain their existing presentation path.
     */
    static boolean usesNativeCameraAnchor(boolean renderOverrideActive,
                                          boolean frameAuthoritative,
                                          boolean builtInRigActive,
                                          boolean directViewProxyActive) {
        return renderOverrideActive && frameAuthoritative
                && !builtInRigActive && !directViewProxyActive;
    }

    static CameraVector nativeCameraOriginTranslation(CameraVector renderBase,
                                                      float effectiveEyeHeight,
                                                      CameraVector targetPosition) {
        return new CameraVector(renderBase.x - targetPosition.x,
                renderBase.y + effectiveEyeHeight - targetPosition.y,
                renderBase.z - targetPosition.z);
    }

    static CameraVector adjustActiveRenderInfoOrigin(CameraVector nativePosition,
                                                     CameraVector previousBase,
                                                     CameraVector renderBase) {
        return nativePosition.add(renderBase.subtract(previousBase));
    }

    static CameraVector interpolateNativePosition(CameraVector previous,
                                                  CameraVector current,
                                                  double partialTicks) {
        return previous.add(current.subtract(previous).scale(partialTicks));
    }

    static boolean nativeFrontRotationApplied(int vanillaPerspective) {
        return vanillaPerspective == 2;
    }

    public static double vanillaThirdPersonDistance(double computedDistance,
                                                     boolean suppressDisplacement) {
        return suppressDisplacement ? 0.0D : computedDistance;
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
