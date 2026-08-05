package neofontrender.addons.flight;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

final class FlightRollConfig {
    static boolean enabled = false;
    static boolean momentumMouse = true;
    static float rollSensitivity = 1.0F;
    static float pitchSensitivity = 1.0F;
    static float maximumRollSpeed = 180.0F;
    static int momentumDeadzonePercent = 5;
    static boolean invertPitch;
    static boolean invertYaw;
    static boolean invertRoll;
    static float controllerPitchSensitivity = 1.0F;
    static float controllerYawSensitivity = 0.4F;
    static float controllerRollSensitivity = 1.0F;
    static boolean barrelRolls = true;
    static int barrelDurationTicks = 14;
    static boolean remotePlayerRoll = true;
    static boolean flightHud = true;
    static String hudTheme = "airbus-a350";
    static String hudSpeedUnit = "KNOTS";
    static String hudAltitudeUnit = "FEET";
    static String hudVerticalSpeedUnit = "FPM";
    static boolean hudHorizon = true;
    static boolean hudInputIndicator = true;
    static int hudScalePercent = 100;
    static boolean hudHideHotbar;
    static boolean hudHidePlayerStatus;
    static boolean hudHideExperience;
    static boolean hudHideChat;
    static boolean hudHideBossBars;
    static boolean hudHidePotionIcons;
    static boolean hudHideSubtitles;
    static boolean hudHidePlayerList;
    static boolean hudHideText;
    static boolean hudHideFirstPersonHand;

    private FlightRollConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("flightRoll.enabled", false, "Enable continuous three-axis elytra control.")
                .define("flightRoll.momentumMouse", true,
                        "Use mouse displacement as a persistent virtual flight stick.")
                .define("flightRoll.rollSensitivity", 1.0D, "Horizontal roll sensitivity.")
                .define("flightRoll.pitchSensitivity", 1.0D, "Vertical pitch sensitivity.")
                .define("flightRoll.maximumRollSpeed", 180.0D,
                        "Maximum momentum-mode roll speed in degrees per second.")
                .define("flightRoll.momentumDeadzonePercent", 5,
                        "Virtual flight-stick deadzone percentage.")
                .define("flightRoll.invertPitch", false, "Invert pitch input.")
                .define("flightRoll.invertYaw", false, "Invert yaw input.")
                .define("flightRoll.invertRoll", false, "Invert roll input.")
                .define("flightRoll.controllerPitchSensitivity", 1.0D,
                        "Pitch sensitivity for FlightControllerInputEvent providers.")
                .define("flightRoll.controllerYawSensitivity", 0.4D,
                        "Yaw sensitivity for FlightControllerInputEvent providers.")
                .define("flightRoll.controllerRollSensitivity", 1.0D,
                        "Roll sensitivity for FlightControllerInputEvent providers.")
                .define("flightRoll.barrelRolls", true, "Enable the left/right barrel-roll keys.")
                .define("flightRoll.barrelDurationTicks", 14, "Barrel-roll animation duration.")
                .define("flightRoll.remotePlayerRoll", true,
                        "Render roll values synchronized by the UIE server companion.")
                .define("flightRoll.hud.enabled", true, "Show the Arc3D flight HUD while gliding.")
                .define("flightRoll.hud.theme", "airbus-a350",
                        "Built-in or user-authored JSON flight HUD theme id.")
                .define("flightRoll.hud.speedUnit", "KNOTS", "KNOTS, KPH, MPS, or BPS.")
                .define("flightRoll.hud.altitudeUnit", "FEET", "FEET, METERS, or BLOCKS.")
                .define("flightRoll.hud.verticalSpeedUnit", "FPM", "FPM, MPS, or BPS.")
                .define("flightRoll.hud.horizon", true, "Show the artificial horizon.")
                .define("flightRoll.hud.inputIndicator", true,
                        "Show momentum/controller input position.")
                .define("flightRoll.hud.scalePercent", 100, "Flight HUD scale percentage.")
                .define("flightRoll.hud.hideHotbar", false,
                        "Hide the vanilla hotbar while the flight HUD is visible.")
                .define("flightRoll.hud.hidePlayerStatus", false,
                        "Hide health, armor, food, air, mount health, and jump bars while flying.")
                .define("flightRoll.hud.hideExperience", false,
                        "Hide the experience bar while the flight HUD is visible.")
                .define("flightRoll.hud.hideChat", false,
                        "Hide chat while the flight HUD is visible.")
                .define("flightRoll.hud.hideBossBars", false,
                        "Hide boss bars while the flight HUD is visible.")
                .define("flightRoll.hud.hidePotionIcons", false,
                        "Hide potion-effect icons while the flight HUD is visible.")
                .define("flightRoll.hud.hideSubtitles", false,
                        "Hide subtitles while the flight HUD is visible.")
                .define("flightRoll.hud.hidePlayerList", false,
                        "Hide the player list while the flight HUD is visible.")
                .define("flightRoll.hud.hideText", false,
                        "Hide vanilla overlay text while the flight HUD is visible.")
                .define("flightRoll.hud.hideFirstPersonHand", false,
                        "Hide the first-person hand while the flight HUD is visible.");
        enabled = file.getBoolean("flightRoll.enabled", false);
        momentumMouse = file.getBoolean("flightRoll.momentumMouse", true);
        rollSensitivity = (float) file.getDouble("flightRoll.rollSensitivity", 1.0D, 0.1D, 4.0D);
        pitchSensitivity = (float) file.getDouble("flightRoll.pitchSensitivity", 1.0D, 0.1D, 4.0D);
        maximumRollSpeed = (float) file.getDouble("flightRoll.maximumRollSpeed", 180.0D, 30.0D, 720.0D);
        momentumDeadzonePercent = file.getInt("flightRoll.momentumDeadzonePercent", 5, 0, 30);
        invertPitch = file.getBoolean("flightRoll.invertPitch", false);
        invertYaw = file.getBoolean("flightRoll.invertYaw", false);
        invertRoll = file.getBoolean("flightRoll.invertRoll", false);
        controllerPitchSensitivity = (float) file.getDouble(
                "flightRoll.controllerPitchSensitivity", 1.0D, 0.1D, 4.0D);
        controllerYawSensitivity = (float) file.getDouble(
                "flightRoll.controllerYawSensitivity", 0.4D, 0.1D, 4.0D);
        controllerRollSensitivity = (float) file.getDouble(
                "flightRoll.controllerRollSensitivity", 1.0D, 0.1D, 4.0D);
        barrelRolls = file.getBoolean("flightRoll.barrelRolls", true);
        barrelDurationTicks = file.getInt("flightRoll.barrelDurationTicks", 14, 6, 40);
        remotePlayerRoll = file.getBoolean("flightRoll.remotePlayerRoll", true);
        flightHud = file.getBoolean("flightRoll.hud.enabled", true);
        hudTheme = file.getString("flightRoll.hud.theme", "airbus-a350");
        if ("airbus-color".equals(hudTheme) || "minimal".equals(hudTheme)) hudTheme = "airbus-a350";
        else if ("boeing-color".equals(hudTheme)) hudTheme = "boeing-737";
        hudSpeedUnit = normalize(file.getString("flightRoll.hud.speedUnit", "KNOTS"),
                new String[] {"KNOTS", "KPH", "MPS", "BPS"}, "KNOTS");
        hudAltitudeUnit = normalize(file.getString("flightRoll.hud.altitudeUnit", "FEET"),
                new String[] {"FEET", "METERS", "BLOCKS"}, "FEET");
        hudVerticalSpeedUnit = normalize(file.getString("flightRoll.hud.verticalSpeedUnit", "FPM"),
                new String[] {"FPM", "MPS", "BPS"}, "FPM");
        hudHorizon = file.getBoolean("flightRoll.hud.horizon", true);
        hudInputIndicator = file.getBoolean("flightRoll.hud.inputIndicator", true);
        hudScalePercent = file.getInt("flightRoll.hud.scalePercent", 100, 50, 100);
        hudHideHotbar = file.getBoolean("flightRoll.hud.hideHotbar", false);
        hudHidePlayerStatus = file.getBoolean("flightRoll.hud.hidePlayerStatus", false);
        hudHideExperience = file.getBoolean("flightRoll.hud.hideExperience", false);
        hudHideChat = file.getBoolean("flightRoll.hud.hideChat", false);
        hudHideBossBars = file.getBoolean("flightRoll.hud.hideBossBars", false);
        hudHidePotionIcons = file.getBoolean("flightRoll.hud.hidePotionIcons", false);
        hudHideSubtitles = file.getBoolean("flightRoll.hud.hideSubtitles", false);
        hudHidePlayerList = file.getBoolean("flightRoll.hud.hidePlayerList", false);
        hudHideText = file.getBoolean("flightRoll.hud.hideText", false);
        hudHideFirstPersonHand = file.getBoolean(
                "flightRoll.hud.hideFirstPersonHand", false);
        file.save();
    }

    static void save() {
        rollSensitivity = Math.max(0.1F, Math.min(4.0F, rollSensitivity));
        pitchSensitivity = Math.max(0.1F, Math.min(4.0F, pitchSensitivity));
        maximumRollSpeed = Math.max(30.0F, Math.min(720.0F, maximumRollSpeed));
        momentumDeadzonePercent = Math.max(0, Math.min(30, momentumDeadzonePercent));
        controllerPitchSensitivity = clampSensitivity(controllerPitchSensitivity);
        controllerYawSensitivity = clampSensitivity(controllerYawSensitivity);
        controllerRollSensitivity = clampSensitivity(controllerRollSensitivity);
        hudScalePercent = Math.max(50, Math.min(100, hudScalePercent));
        barrelDurationTicks = Math.max(6, Math.min(40, barrelDurationTicks));
        UiEnhancementsConfig.file()
                .set("flightRoll.enabled", enabled)
                .set("flightRoll.momentumMouse", momentumMouse)
                .set("flightRoll.rollSensitivity", (double) rollSensitivity)
                .set("flightRoll.pitchSensitivity", (double) pitchSensitivity)
                .set("flightRoll.maximumRollSpeed", (double) maximumRollSpeed)
                .set("flightRoll.momentumDeadzonePercent", momentumDeadzonePercent)
                .set("flightRoll.invertPitch", invertPitch)
                .set("flightRoll.invertYaw", invertYaw)
                .set("flightRoll.invertRoll", invertRoll)
                .set("flightRoll.controllerPitchSensitivity", (double) controllerPitchSensitivity)
                .set("flightRoll.controllerYawSensitivity", (double) controllerYawSensitivity)
                .set("flightRoll.controllerRollSensitivity", (double) controllerRollSensitivity)
                .set("flightRoll.barrelRolls", barrelRolls)
                .set("flightRoll.barrelDurationTicks", barrelDurationTicks)
                .set("flightRoll.remotePlayerRoll", remotePlayerRoll)
                .set("flightRoll.hud.enabled", flightHud)
                .set("flightRoll.hud.theme", hudTheme)
                .set("flightRoll.hud.speedUnit", hudSpeedUnit)
                .set("flightRoll.hud.altitudeUnit", hudAltitudeUnit)
                .set("flightRoll.hud.verticalSpeedUnit", hudVerticalSpeedUnit)
                .set("flightRoll.hud.horizon", hudHorizon)
                .set("flightRoll.hud.inputIndicator", hudInputIndicator)
                .set("flightRoll.hud.scalePercent", hudScalePercent)
                .set("flightRoll.hud.hideHotbar", hudHideHotbar)
                .set("flightRoll.hud.hidePlayerStatus", hudHidePlayerStatus)
                .set("flightRoll.hud.hideExperience", hudHideExperience)
                .set("flightRoll.hud.hideChat", hudHideChat)
                .set("flightRoll.hud.hideBossBars", hudHideBossBars)
                .set("flightRoll.hud.hidePotionIcons", hudHidePotionIcons)
                .set("flightRoll.hud.hideSubtitles", hudHideSubtitles)
                .set("flightRoll.hud.hidePlayerList", hudHidePlayerList)
                .set("flightRoll.hud.hideText", hudHideText)
                .set("flightRoll.hud.hideFirstPersonHand", hudHideFirstPersonHand)
                .save();
    }

    private static float clampSensitivity(float value) {
        return Math.max(0.1F, Math.min(4.0F, value));
    }

    private static String normalize(String value, String[] allowed, String fallback) {
        String normalized = value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
        for (String candidate : allowed) if (candidate.equals(normalized)) return candidate;
        return fallback;
    }
}
