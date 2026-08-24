package neofontrender.addons.camera;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

/** Omnilook-compatible free-look behavior, expressed without Euler state. */
final class FreeLookConfig {
    static boolean toggleMode = true;
    static double pitchLimitDegrees = 90.0D;
    static double orientationResponse = 18.0D;
    static double mouseResponse = 1.0D;
    static double rollSpeedDegrees = 180.0D;
    static double distance = 4.0D;
    static boolean collision = true;
    static boolean controlPlayerByDefault = false;
    static double moveStepSize = 0.25D;

    private FreeLookConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("camera.freelook.toggleMode", true,
                "Toggle when pressed; when false, hold the free-look key.");
        file.define("camera.freelook.pitchLimitDegrees", 90.0D,
                "Absolute quaternion free-look pitch limit.");
        file.define("camera.freelook.orientationResponse", 18.0D,
                "Frame-rate independent orientation inertia response.");
        file.define("camera.freelook.mouseResponse", 1.0D,
                "Multiplier applied after vanilla mouse sensitivity and Omnilook's 0.15 turn scale.");
        file.define("camera.freelook.rollSpeedDegrees", 180.0D,
                "Free-look roll speed for CAMERA_ROLL input.");
        file.define("camera.freelook.distance", 4.0D,
                "Third-person orbit distance used by the detached quaternion camera.");
        file.define("camera.freelook.collision", true,
                "Clamp the free-look orbit camera against blocks.");
        file.define("camera.freelook.controlPlayerByDefault", false,
                "When true, free-look starts in player-control mode (mouse rotates player body). Toggle with key.");
        file.define("camera.freelook.moveStepSize", 0.25D,
                "Step size per keypress for free-look camera position movement (numpad keys).");
        toggleMode = file.getBoolean("camera.freelook.toggleMode", true);
        pitchLimitDegrees = file.getDouble("camera.freelook.pitchLimitDegrees",
                90.0D, 1.0D, 90.0D);
        orientationResponse = file.getDouble("camera.freelook.orientationResponse",
                18.0D, 0.0D, 120.0D);
        mouseResponse = file.getDouble("camera.freelook.mouseResponse", 1.0D, 0.0D, 4.0D);
        rollSpeedDegrees = file.getDouble("camera.freelook.rollSpeedDegrees",
                180.0D, 0.0D, 720.0D);
        distance = file.getDouble("camera.freelook.distance", 4.0D, 0.0D, 32.0D);
        collision = file.getBoolean("camera.freelook.collision", true);
        controlPlayerByDefault = file.getBoolean("camera.freelook.controlPlayerByDefault", false);
        moveStepSize = file.getDouble("camera.freelook.moveStepSize", 0.25D, 0.01D, 5.0D);
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("camera.freelook.toggleMode", toggleMode)
                .set("camera.freelook.pitchLimitDegrees", pitchLimitDegrees)
                .set("camera.freelook.orientationResponse", orientationResponse)
                .set("camera.freelook.mouseResponse", mouseResponse)
                .set("camera.freelook.rollSpeedDegrees", rollSpeedDegrees)
                .set("camera.freelook.distance", distance)
                .set("camera.freelook.collision", collision)
                .set("camera.freelook.controlPlayerByDefault", controlPlayerByDefault)
                .set("camera.freelook.moveStepSize", moveStepSize)
                .save();
    }
}
