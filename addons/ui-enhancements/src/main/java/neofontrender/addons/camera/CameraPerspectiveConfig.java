package neofontrender.addons.camera;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

/** Controls which UIE rigs participate in the vanilla perspective/F5 cycle. */
final class CameraPerspectiveConfig {
    static boolean f5CycleEnabled = true;
    static boolean shoulderInF5 = true;
    static boolean freeLookInF5 = true;
    static boolean droneInF5 = true;
    static boolean replaceDefaultPerspective;
    static boolean skipThirdPersonFront;
    static boolean rememberLastPerspective = true;
    static int defaultPerspective;
    static String defaultMode = "vanilla_first";

    private CameraPerspectiveConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("camera.f5Cycle.enabled", true,
                "Insert UIE camera modes into vanilla F5 perspective cycling.");
        file.define("camera.f5Cycle.shoulder", true,
                "Include Shoulder view in the F5 cycle.");
        file.define("camera.f5Cycle.freeLook", true,
                "Include free-look view in the F5 cycle.");
        file.define("camera.f5Cycle.drone", true,
                "Include Drone view in the F5 cycle.");
        file.define("camera.f5Cycle.replaceDefaultPerspective", false,
                "Use Shoulder in the vanilla third-person-back slot.");
        file.define("camera.f5Cycle.skipThirdPersonFront", false,
                "Skip vanilla front-facing third person.");
        file.define("camera.f5Cycle.rememberLastPerspective", true,
                "Remember the last selected F5 mode.");
        file.define("camera.f5Cycle.defaultPerspective", 0,
                "Initial perspective: 0 first, 1 third, 2 front.");
        file.define("camera.f5Cycle.defaultMode", "vanilla_first",
                "Initial F5 mode, including UIE camera modes.");
        f5CycleEnabled = file.getBoolean("camera.f5Cycle.enabled", true);
        shoulderInF5 = file.getBoolean("camera.f5Cycle.shoulder", true);
        freeLookInF5 = file.getBoolean("camera.f5Cycle.freeLook", true);
        droneInF5 = file.getBoolean("camera.f5Cycle.drone", true);
        replaceDefaultPerspective = file.getBoolean("camera.f5Cycle.replaceDefaultPerspective", false);
        skipThirdPersonFront = file.getBoolean("camera.f5Cycle.skipThirdPersonFront", false);
        rememberLastPerspective = file.getBoolean("camera.f5Cycle.rememberLastPerspective", true);
        defaultPerspective = file.getInt("camera.f5Cycle.defaultPerspective", 0, 0, 2);
        defaultMode = normalizeMode(file.getString("camera.f5Cycle.defaultMode",
                vanillaMode(defaultPerspective)));
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("camera.f5Cycle.enabled", f5CycleEnabled)
                .set("camera.f5Cycle.shoulder", shoulderInF5)
                .set("camera.f5Cycle.freeLook", freeLookInF5)
                .set("camera.f5Cycle.drone", droneInF5)
                .set("camera.f5Cycle.replaceDefaultPerspective", replaceDefaultPerspective)
                .set("camera.f5Cycle.skipThirdPersonFront", skipThirdPersonFront)
                .set("camera.f5Cycle.rememberLastPerspective", rememberLastPerspective)
                .set("camera.f5Cycle.defaultPerspective", defaultPerspective)
                .set("camera.f5Cycle.defaultMode", defaultMode)
                .save();
    }

    static String normalizeMode(String value) {
        if (value == null) return "vanilla_first";
        switch (value) {
            case "vanilla_third": case "shoulder": case "free_look":
            case "drone": case "vanilla_front": return value;
            default: return "vanilla_first";
        }
    }

    private static String vanillaMode(int perspective) {
        return perspective == 1 ? "vanilla_third" : perspective == 2
                ? "vanilla_front" : "vanilla_first";
    }
}
