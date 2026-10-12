package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraVector;

/** Pure quaternion fly-camera integrator. It never references or mutates player state. */
final class DroneMotionController {
    private static final double MAX_FRAME_SECONDS = 0.05D;
    private CameraVector position;
    private CameraAttitude attitude;
    private CameraVector velocity = new CameraVector(0.0D, 0.0D, 0.0D);

    DroneMotionController(CameraVector position, CameraAttitude attitude) {
        this.position = position;
        this.attitude = attitude;
    }

    void look(int deltaX, int deltaY, boolean invertMouse) {
        double yaw = -deltaX * DroneCameraConfig.lookSensitivity;
        double pitch = deltaY * DroneCameraConfig.lookSensitivity * (invertMouse ? -1.0D : 1.0D);
        CameraAttitude yawDelta = CameraAttitude.axisAngle(new CameraVector(0.0D, 1.0D, 0.0D), yaw);
        CameraAttitude pitchDelta = CameraAttitude.axisAngle(new CameraVector(1.0D, 0.0D, 0.0D), pitch);
        attitude = yawDelta.multiply(attitude).multiply(pitchDelta);
    }

    void move(double localX, double localY, double localZ, double frameSeconds) {
        double dt = Math.max(0.0D, Math.min(MAX_FRAME_SECONDS, frameSeconds));
        CameraVector command = new CameraVector(localX, localY, localZ);
        CameraVector targetVelocity = attitude.rotate(command.scale(DroneCameraConfig.speed));
        double blend = DroneCameraConfig.translationResponse <= 0.0D ? 1.0D
                : 1.0D - Math.exp(-DroneCameraConfig.translationResponse * dt);
        velocity = velocity.scale(1.0D - blend).add(targetVelocity.scale(blend));
        position = position.add(velocity.scale(dt));
    }

    void roll(double axis, double frameSeconds) {
        double dt = Math.max(0.0D, Math.min(MAX_FRAME_SECONDS, frameSeconds));
        if (Math.abs(axis) < 1.0E-6D || dt == 0.0D) return;
        attitude = attitude.multiply(CameraAttitude.axisAngle(
                new CameraVector(0.0D, 0.0D, 1.0D),
                axis * Math.toRadians(DroneCameraConfig.rollSpeedDegrees) * dt));
    }

    void setPosition(CameraVector position) { this.position = position; }
    void stop() { velocity = new CameraVector(0.0D, 0.0D, 0.0D); }

    CameraVector position() { return position; }
    CameraAttitude attitude() { return attitude; }
    CameraVector velocity() { return velocity; }
}
