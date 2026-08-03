package neofontrender.addons.zoom;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

final class ZoomConfig {
    static boolean enabled = true;
    static float magnification = 4.0F;
    static boolean smoothCamera = false;
    static int mouseSensitivityAdjustmentPercent = 0;
    static boolean smoothTransition = true;
    static int transitionDurationMillis = 200;

    private ZoomConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("zoom.enabled", true, "Enable the hold-to-zoom key binding.")
                .define("zoom.magnification", 4.0D, "FOV magnification while the zoom key is held (2-8).")
                .define("zoom.smoothCamera", false, "Temporarily enable smooth camera while zooming.")
                .define("zoom.mouseSensitivityAdjustmentPercent", 0,
                        "Zoom mouse adjustment (-100 decreases, 0 preserves, 100 increases).")
                .define("zoom.smoothTransition", true, "Animate FOV changes when entering and leaving zoom.")
                .define("zoom.transitionDurationMillis", 200, "Zoom transition duration in milliseconds (40-1000).");
        enabled = file.getBoolean("zoom.enabled", true);
        magnification = (float) file.getDouble("zoom.magnification", 4.0D, 2.0D, 8.0D);
        smoothCamera = file.getBoolean("zoom.smoothCamera", false);
        mouseSensitivityAdjustmentPercent = file.getInt(
                "zoom.mouseSensitivityAdjustmentPercent", 0, -100, 100);
        smoothTransition = file.getBoolean("zoom.smoothTransition", true);
        transitionDurationMillis = file.getInt("zoom.transitionDurationMillis", 200, 40, 1000);
        file.save();
    }

    static void save() {
        magnification = ZoomMath.clampMagnification(magnification);
        mouseSensitivityAdjustmentPercent = Math.max(-100,
                Math.min(100, mouseSensitivityAdjustmentPercent));
        transitionDurationMillis = Math.max(40, Math.min(1000, transitionDurationMillis));
        UiEnhancementsConfig.file()
                .set("zoom.enabled", enabled)
                .set("zoom.magnification", (double) magnification)
                .set("zoom.smoothCamera", smoothCamera)
                .set("zoom.mouseSensitivityAdjustmentPercent", mouseSensitivityAdjustmentPercent)
                .set("zoom.smoothTransition", smoothTransition)
                .set("zoom.transitionDurationMillis", transitionDurationMillis)
                .save();
    }
}
