package neofontrender.addons.flight;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

final class FlightRollConfig {
    static boolean enabled = false;
    static boolean allowInWater = false;
    static boolean keyboardYaw = false;
    static boolean wsPitch = true;
    static boolean banking = false;
    static boolean momentumMouse = true;
    static float rollSensitivity = 1.0F;
    static float pitchSensitivity = 1.0F;
    static float yawSensitivity = 0.4F;
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
    static boolean hudMaskEnabled = false;
    static int hudMaskColor = 0xFF808080;
    static int hudMaskOpacityPercent = 15;

    private FlightRollConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("flightRoll.enabled", false, "Enable continuous three-axis elytra control.")
                .define("flightRoll.allowInWater", false,
                        "Keep flight-roll control active while the player is in water.")
                .define("flightRoll.keyboardYaw", false,
                        "Enable the remappable left/right keyboard yaw bindings.")
                .define("flightRoll.wsPitch", true,
                        "Map the forward/back movement keys (W/S) to the pitch axis while flying.")
                .define("flightRoll.banking", false,
                        "Automatically coordinate turns from the current roll attitude.")
                .define("flightRoll.momentumMouse", true,
                        "Use mouse displacement as a persistent virtual flight stick.")
                .define("flightRoll.rollSensitivity", 1.0D, "Horizontal roll sensitivity.")
                .define("flightRoll.pitchSensitivity", 1.0D, "Vertical pitch sensitivity.")
                .define("flightRoll.yawSensitivity", 0.4D,
                        "Keyboard yaw sensitivity for the left/right yaw bindings.")
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
                .define("flightRoll.hud.mask.enabled", false,
                        "Draw a configurable translucent mask behind the flight HUD.")
                .define("flightRoll.hud.mask.color", "#FF808080",
                        "Flight HUD mask RGB color.")
                .define("flightRoll.hud.mask.opacityPercent", 15,
                        "Flight HUD mask opacity percentage.")
                ;
        enabled = file.getBoolean("flightRoll.enabled", false);
        allowInWater = file.getBoolean("flightRoll.allowInWater", false);
        keyboardYaw = file.getBoolean("flightRoll.keyboardYaw", false);
        wsPitch = file.getBoolean("flightRoll.wsPitch", true);
        banking = file.getBoolean("flightRoll.banking", false);
        momentumMouse = file.getBoolean("flightRoll.momentumMouse", true);
        rollSensitivity = (float) file.getDouble("flightRoll.rollSensitivity", 1.0D, 0.1D, 4.0D);
        pitchSensitivity = (float) file.getDouble("flightRoll.pitchSensitivity", 1.0D, 0.1D, 4.0D);
        yawSensitivity = (float) file.getDouble("flightRoll.yawSensitivity", 0.4D, 0.1D, 4.0D);
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
        hudMaskEnabled = file.getBoolean("flightRoll.hud.mask.enabled", false);
        hudMaskColor = parseColor(file.getString("flightRoll.hud.mask.color", "#FF808080"),
                0xFF808080);
        hudMaskOpacityPercent = file.getInt("flightRoll.hud.mask.opacityPercent", 15, 0, 100);
        file.save();
    }

    static void save() {
        rollSensitivity = Math.max(0.1F, Math.min(4.0F, rollSensitivity));
        pitchSensitivity = Math.max(0.1F, Math.min(4.0F, pitchSensitivity));
        yawSensitivity = Math.max(0.1F, Math.min(4.0F, yawSensitivity));
        maximumRollSpeed = Math.max(30.0F, Math.min(720.0F, maximumRollSpeed));
        momentumDeadzonePercent = Math.max(0, Math.min(30, momentumDeadzonePercent));
        controllerPitchSensitivity = clampSensitivity(controllerPitchSensitivity);
        controllerYawSensitivity = clampSensitivity(controllerYawSensitivity);
        controllerRollSensitivity = clampSensitivity(controllerRollSensitivity);
        hudScalePercent = Math.max(50, Math.min(100, hudScalePercent));
        hudMaskColor = 0xFF000000 | (hudMaskColor & 0x00FFFFFF);
        hudMaskOpacityPercent = Math.max(0, Math.min(100, hudMaskOpacityPercent));
        barrelDurationTicks = Math.max(6, Math.min(40, barrelDurationTicks));
        UiEnhancementsConfig.file()
                .set("flightRoll.enabled", enabled)
                .set("flightRoll.allowInWater", allowInWater)
                .set("flightRoll.keyboardYaw", keyboardYaw)
                .set("flightRoll.wsPitch", wsPitch)
                .set("flightRoll.banking", banking)
                .set("flightRoll.momentumMouse", momentumMouse)
                .set("flightRoll.rollSensitivity", (double) rollSensitivity)
                .set("flightRoll.pitchSensitivity", (double) pitchSensitivity)
                .set("flightRoll.yawSensitivity", (double) yawSensitivity)
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
                .set("flightRoll.hud.mask.enabled", hudMaskEnabled)
                .set("flightRoll.hud.mask.color", String.format("#%08X", hudMaskColor))
                .set("flightRoll.hud.mask.opacityPercent", hudMaskOpacityPercent)
                .save();
    }

    static int hudMaskArgb() {
        int alpha = (hudMaskOpacityPercent * 255 + 50) / 100;
        return (hudMaskColor & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private static float clampSensitivity(float value) {
        return Math.max(0.1F, Math.min(4.0F, value));
    }

    private static String normalize(String value, String[] allowed, String fallback) {
        String normalized = value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
        for (String candidate : allowed) if (candidate.equals(normalized)) return candidate;
        return fallback;
    }

    private static int parseColor(String value, int fallback) {
        if (value == null) return fallback;
        String normalized = value.trim();
        if (normalized.startsWith("#")) normalized = normalized.substring(1);
        else if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }
        try {
            long parsed = Long.parseLong(normalized, 16);
            if (normalized.length() <= 6) parsed |= 0xFF000000L;
            return (int) parsed;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
