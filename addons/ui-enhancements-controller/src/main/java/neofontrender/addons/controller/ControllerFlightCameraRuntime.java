package neofontrender.addons.controller;

import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.controller.sdl.ControllerSnapshot;
import neofontrender.addons.controller.sdl.SdlDeviceManager;

/** MSFS-style flight freelook: camera attitude is independent from the aircraft control axes. */
final class ControllerFlightCameraRuntime {
    private static final double YAW_DEGREES_PER_SECOND = 120.0D;
    private static final double PITCH_DEGREES_PER_SECOND = 100.0D;
    private final SdlDeviceManager manager;
    private double yawOffset;
    private double pitchOffset;
    private long lastFrameNanos;

    ControllerFlightCameraRuntime(SdlDeviceManager manager) {
        this.manager = manager;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void cameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (!shouldApply(FlightApi.isActive(), CameraApi.isDroneActive(),
                CameraApi.isFreeLookActive())) {
            reset();
            return;
        }
        ControllerSnapshot snapshot = manager.latestSnapshot();
        if (!snapshot.isConnected()) {
            lastFrameNanos = 0L;
            return;
        }
        long now = System.nanoTime();
        double seconds = lastFrameNanos == 0L ? 0.0D
                : Math.min(0.05D, Math.max(0.0D,
                (now - lastFrameNanos) / 1_000_000_000.0D));
        lastFrameNanos = now;
        float lookX = ControllerBindings.resolve(
                InputAction.CAMERA_LOOK_X, snapshot).getAxis();
        float lookY = ControllerBindings.resolve(
                InputAction.CAMERA_LOOK_Y, snapshot).getAxis();
        if (ControllerBindings.resolve(InputAction.CAMERA_TOGGLE_FREELOOK_CONTROL, snapshot)
                .isPressed()) {
            yawOffset = 0.0D;
            pitchOffset = 0.0D;
        }
        yawOffset = wrap(yawOffset + lookX * YAW_DEGREES_PER_SECOND * seconds);
        pitchOffset = clamp(pitchOffset + lookY * PITCH_DEGREES_PER_SECOND * seconds,
                -85.0D, 85.0D);
        event.setYaw((float) (event.getYaw() + yawOffset));
        event.setPitch((float) clamp(event.getPitch() + pitchOffset, -90.0D, 90.0D));
    }

    static boolean shouldApply(boolean flight, boolean drone, boolean freeLook) {
        return flight && !drone && !freeLook;
    }

    private void reset() {
        yawOffset = 0.0D;
        pitchOffset = 0.0D;
        lastFrameNanos = 0L;
    }

    private static double wrap(double value) {
        double wrapped = value % 360.0D;
        if (wrapped >= 180.0D) wrapped -= 360.0D;
        if (wrapped < -180.0D) wrapped += 360.0D;
        return wrapped;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
