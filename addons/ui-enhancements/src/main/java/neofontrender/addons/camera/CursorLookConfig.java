package neofontrender.addons.camera;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

/** Settings owned by the independent cursor-look camera mode. */
final class CursorLookConfig {
    static double speed = 1.0D;
    static double aimDistance = 400.0D;
    static boolean useShoulderOffset = true;
    static boolean headOnlyAim;
    static boolean cameraRelativeMovement;

    private CursorLookConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("camera.cursorLook.speed", 1.0D,
                "Multiplier applied to raw mouse movement while moving the aim cursor.");
        file.define("camera.cursorLook.aimDistance", 400.0D,
                "World distance used to align player and ranged weapons when the cursor has no reachable hit.");
        file.define("camera.cursorLook.useShoulderOffset", true,
                "Place cursor-look with the configured Shoulder camera offset and collision.");
        file.define("camera.cursorLook.headOnlyAim", false,
                "Keep body yaw stable while cursor aiming fits within the natural head-turn range.");
        file.define("camera.cursorLook.cameraRelativeMovement", false,
                "Rotate player movement input so WASD remains relative to the detached camera.");
        speed = file.getDouble("camera.cursorLook.speed", 1.0D, 0.01D, 4.0D);
        aimDistance = file.getDouble("camera.cursorLook.aimDistance", 400.0D, 16.0D, 4096.0D);
        useShoulderOffset = file.getBoolean("camera.cursorLook.useShoulderOffset", true);
        headOnlyAim = file.getBoolean("camera.cursorLook.headOnlyAim", false);
        cameraRelativeMovement = file.getBoolean("camera.cursorLook.cameraRelativeMovement", false);
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("camera.cursorLook.speed", speed)
                .set("camera.cursorLook.aimDistance", aimDistance)
                .set("camera.cursorLook.useShoulderOffset", useShoulderOffset)
                .set("camera.cursorLook.headOnlyAim", headOnlyAim)
                .set("camera.cursorLook.cameraRelativeMovement", cameraRelativeMovement)
                .save();
    }
}
