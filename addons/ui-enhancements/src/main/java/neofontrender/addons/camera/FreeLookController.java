package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraVector;

/** Absolute quaternion view controller; it never writes or follows the player's physical pose. */
final class FreeLookController {
    private static final double OMNILOOK_DEGREES_PER_SCALED_UNIT = 0.15D;
    private static final double MAX_FRAME_SECONDS = 0.05D;
    private CameraAttitude target;
    private CameraAttitude rendered;
    private double pitchDegrees;
    private long lastResolveNanos;

    FreeLookController() { this(CameraAttitude.IDENTITY); }

    FreeLookController(CameraAttitude initial) {
        target = initial == null ? CameraAttitude.IDENTITY : initial;
        rendered = target;
        pitchDegrees = -Math.toDegrees(Math.asin(Math.max(-1.0D,
                Math.min(1.0D, target.forward().y))));
    }

    void look(int deltaX, int deltaY, boolean invertMouse, double sensitivity) {
        double scaled = vanillaMouseScale(sensitivity) * OMNILOOK_DEGREES_PER_SCALED_UNIT
                * FreeLookConfig.mouseResponse;
        double yaw = Math.toRadians(-deltaX * scaled);
        double requestedPitch = Math.toRadians(deltaY * scaled
                * (invertMouse ? -1.0D : 1.0D));
        double previousPitch = pitchDegrees;
        pitchDegrees = Math.max(-FreeLookConfig.pitchLimitDegrees,
                Math.min(FreeLookConfig.pitchLimitDegrees,
                        previousPitch + Math.toDegrees(requestedPitch)));
        double pitch = Math.toRadians(pitchDegrees - previousPitch);
        target = CameraAttitude.axisAngle(new CameraVector(0.0D, 1.0D, 0.0D), yaw)
                .multiply(target)
                .multiply(CameraAttitude.axisAngle(new CameraVector(1.0D, 0.0D, 0.0D), pitch));
    }

    void roll(double axis, double frameSeconds) {
        double dt = Math.max(0.0D, Math.min(MAX_FRAME_SECONDS, frameSeconds));
        if (Math.abs(axis) < 1.0E-6D || dt == 0.0D) return;
        target = target.multiply(CameraAttitude.axisAngle(
                new CameraVector(0.0D, 0.0D, 1.0D),
                axis * Math.toRadians(FreeLookConfig.rollSpeedDegrees) * dt));
    }

    static double vanillaMouseScale(double sensitivity) {
        double value = sensitivity * 0.6D + 0.2D;
        return value * value * value * 8.0D;
    }

    CameraAttitude resolve(CameraAttitude ignoredBody) {
        long now = System.nanoTime();
        double dt = lastResolveNanos == 0L ? 1.0D / 60.0D
                : Math.max(0.0D, Math.min(0.1D, (now - lastResolveNanos) / 1_000_000_000.0D));
        lastResolveNanos = now;
        double blend = FreeLookConfig.orientationResponse <= 0.0D ? 1.0D
                : 1.0D - Math.exp(-FreeLookConfig.orientationResponse * dt);
        rendered = rendered.slerp(target, blend);
        return rendered;
    }

    /** Immediately snaps both target and rendered attitude, used when switching control modes. */
    void syncTo(CameraAttitude attitude) {
        if (attitude == null) return;
        target = attitude;
        rendered = attitude;
        pitchDegrees = -Math.toDegrees(Math.asin(Math.max(-1.0D,
                Math.min(1.0D, attitude.forward().y))));
    }
}
