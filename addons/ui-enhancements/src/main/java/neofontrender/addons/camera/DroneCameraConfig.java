package neofontrender.addons.camera;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

/** Safety policy for detached drone cameras. */
final class DroneCameraConfig {
    static boolean collision = true;
    static boolean allowCameraInteraction = false;
    static double speed = 12.0D;
    static double translationResponse = 10.0D;
    static double lookSensitivity = 0.0025D;
    static double rollSpeedDegrees = 180.0D;

    private DroneCameraConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("camera.drone.collision", true,
                "Clamp detached camera movement against blocks.");
        file.define("camera.drone.allowCameraInteraction", false,
                "Allow interaction from the camera origin; disabled by default.");
        file.define("camera.drone.speed", 12.0D, "Drone translation speed in blocks per second.");
        file.define("camera.drone.translationResponse", 10.0D,
                "Frame-rate independent response for drone translation inertia.");
        file.define("camera.drone.lookSensitivity", 0.0025D,
                "Drone camera radians per mouse unit.");
        file.define("camera.drone.rollSpeedDegrees", 180.0D,
                "Drone roll speed for CAMERA_ROLL input.");
        collision = file.getBoolean("camera.drone.collision", true);
        allowCameraInteraction = file.getBoolean("camera.drone.allowCameraInteraction", false);
        speed = file.getDouble("camera.drone.speed", 12.0D, 0.1D, 256.0D);
        translationResponse = file.getDouble("camera.drone.translationResponse", 10.0D, 0.0D, 120.0D);
        lookSensitivity = file.getDouble("camera.drone.lookSensitivity", 0.0025D, 0.00001D, 0.1D);
        rollSpeedDegrees = file.getDouble("camera.drone.rollSpeedDegrees", 180.0D, 0.0D, 720.0D);
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("camera.drone.collision", collision)
                .set("camera.drone.allowCameraInteraction", allowCameraInteraction)
                .set("camera.drone.speed", speed)
                .set("camera.drone.translationResponse", translationResponse)
                .set("camera.drone.lookSensitivity", lookSensitivity)
                .set("camera.drone.rollSpeedDegrees", rollSpeedDegrees)
                .save();
    }
}
