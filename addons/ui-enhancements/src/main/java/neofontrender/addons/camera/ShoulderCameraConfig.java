package neofontrender.addons.camera;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/** Internal Shoulder Surfing-derived defaults. External Shoulder Surfing always wins detection. */
final class ShoulderCameraConfig {
    static double offsetX = -0.75D;
    static double offsetY;
    /** Original Shoulder Surfing convention: positive distance places the camera behind. */
    static double offsetZ = 3.0D;
    static double minOffsetX = -3.0D, minOffsetY = -1.0D, minOffsetZ = -3.0D;
    static double maxOffsetX = 3.0D, maxOffsetY = 1.5D, maxOffsetZ = 5.0D;
    static boolean unlimitedOffsetX, unlimitedOffsetY, unlimitedOffsetZ;
    static double cameraStepSize = 0.025D;
    static double transitionSpeed = 0.25D;
    static double keepCameraOutOfHeadMultiplier = 0.75D;
    static boolean dynamicallyAdjustOffsets = true;
    static boolean limitPlayerReach = true;
    static boolean useCustomRaytraceDistance = true;
    static double customRaytraceDistance = 400.0D;
    static boolean hidePlayerWhenLookingUp = true;
    static double hidePlayerWhenLookingUpAngle = 15.0D;
    static List<String> adaptiveHoldItems = Arrays.asList(
            "minecraft:snowball", "minecraft:egg", "minecraft:experience_bottle",
            "minecraft:ender_pearl", "minecraft:splash_potion", "minecraft:fishing_rod",
            "minecraft:lingering_potion");
    static List<String> adaptiveUseItems = Collections.emptyList();
    static List<String> adaptiveHoldProperties = Collections.singletonList("minecraft:charged");
    static List<String> adaptiveUseProperties = Arrays.asList("minecraft:pull", "minecraft:throwing");
    static boolean collision = true;
    static boolean centerWhenClimbing = true;
    static double centerWhenLookingDownDegrees = 15.0D;
    static double sprintXMultiplier = 1.0D;
    static double sprintYMultiplier = 1.0D;
    static double sprintZMultiplier = 1.0D;
    static double passengerXMultiplier = 1.0D;
    static double passengerYMultiplier = 1.0D;
    static double passengerZMultiplier = 1.0D;
    static String crosshairMode = "camera";
    static String crosshairType = "dynamic";
    static final Map<Integer, String> crosshairVisibility = new HashMap<>();
    static boolean playerTransparency = true;
    static int playerTransparencyPercent = 100;
    static boolean valkyrienShipCollision;

    private ShoulderCameraConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("camera.shoulder.offsetX", -0.75D, "Local left/right shoulder offset.")
                .define("camera.shoulder.offsetY", 0.0D, "Local vertical shoulder offset.")
                .define("camera.shoulder.offsetZ", 3.0D, "Shoulder distance; positive is behind the player.")
                .define("camera.shoulder.minOffsetX", -3.0D, "Minimum adjustable X offset.")
                .define("camera.shoulder.minOffsetY", -1.0D, "Minimum adjustable Y offset.")
                .define("camera.shoulder.minOffsetZ", -3.0D, "Minimum adjustable Z offset.")
                .define("camera.shoulder.maxOffsetX", 3.0D, "Maximum adjustable X offset.")
                .define("camera.shoulder.maxOffsetY", 1.5D, "Maximum adjustable Y offset.")
                .define("camera.shoulder.maxOffsetZ", 5.0D, "Maximum adjustable Z offset.")
                .define("camera.shoulder.unlimitedOffsetX", false, "Remove X offset limits.")
                .define("camera.shoulder.unlimitedOffsetY", false, "Remove Y offset limits.")
                .define("camera.shoulder.unlimitedOffsetZ", false, "Remove Z offset limits.")
                .define("camera.shoulder.cameraStepSize", 0.025D, "Offset change per adjustment step.")
                .define("camera.shoulder.transitionSpeed", 0.25D, "Offset transition response.")
                .define("camera.shoulder.keepCameraOutOfHeadMultiplier", 0.75D, "Head clearance multiplier.")
                .define("camera.shoulder.dynamicallyAdjustOffsets", true, "Adapt offsets when space is constrained.")
                .define("camera.shoulder.limitPlayerReach", true, "Limit interaction reach to the camera ray.")
                .define("camera.shoulder.useCustomRaytraceDistance", true, "Use custom adaptive ray distance.")
                .define("camera.shoulder.customRaytraceDistance", 400.0D, "Adaptive crosshair ray distance.")
                .define("camera.shoulder.hidePlayerWhenLookingUp", true, "Hide player when camera looks above the body.")
                .define("camera.shoulder.hidePlayerWhenLookingUpAngle", 15.0D, "Upward hide angle; zero disables.")
                .define("camera.shoulder.adaptiveHoldItems", Arrays.asList(
                        "minecraft:snowball", "minecraft:egg", "minecraft:experience_bottle",
                        "minecraft:ender_pearl", "minecraft:splash_potion", "minecraft:fishing_rod",
                        "minecraft:lingering_potion"), "Adaptive crosshair held items.")
                .define("camera.shoulder.adaptiveUseItems", Collections.emptyList(), "Adaptive crosshair active-use items.")
                .define("camera.shoulder.adaptiveHoldProperties", Arrays.asList("minecraft:charged"), "Adaptive held item properties.")
                .define("camera.shoulder.adaptiveUseProperties", Arrays.asList("minecraft:pull", "minecraft:throwing"), "Adaptive active-use properties.")
                .define("camera.shoulder.collision", true, "Clamp the shoulder camera against blocks.")
                .define("camera.shoulder.centerWhenClimbing", true, "Center shoulder view while climbing.")
                .define("camera.shoulder.centerWhenLookingDownDegrees", 15.0D,
                        "Center horizontal shoulder offset close to straight down; zero disables.")
                .define("camera.shoulder.sprintXMultiplier", 1.0D, "Sprint X offset multiplier.")
                .define("camera.shoulder.sprintYMultiplier", 1.0D, "Sprint Y offset multiplier.")
                .define("camera.shoulder.sprintZMultiplier", 1.0D, "Sprint Z offset multiplier.")
                .define("camera.shoulder.passengerXMultiplier", 1.0D, "Passenger X offset multiplier.")
                .define("camera.shoulder.passengerYMultiplier", 1.0D, "Passenger Y offset multiplier.")
                .define("camera.shoulder.passengerZMultiplier", 1.0D, "Passenger Z offset multiplier.")
                .define("camera.shoulder.crosshairMode", "camera",
                        "camera, player, dual, or off projected crosshair routing.")
                .define("camera.shoulder.crosshairType", "dynamic",
                        "ADAPTIVE, DYNAMIC, STATIC, STATIC_WITH_1PP, or DYNAMIC_WITH_1PP.")
                .define("camera.shoulder.visibility.firstPerson", "always", "First-person crosshair visibility rule.")
                .define("camera.shoulder.visibility.thirdPersonBack", "never", "Third-person-back crosshair visibility rule.")
                .define("camera.shoulder.visibility.thirdPersonFront", "never", "Third-person-front crosshair visibility rule.")
                .define("camera.shoulder.visibility.shoulder", "always", "Shoulder crosshair visibility rule.")
                .define("camera.shoulder.playerTransparency", true,
                        "Fade the local player when the shoulder camera intersects the body.")
                .define("camera.shoulder.playerTransparencyPercent", 100,
                        "Scale applied to Shoulder Surfing's geometric local-player alpha.")
                .define("camera.shoulder.valkyrienShipCollision", false,
                        "Allow Shoulder camera collision with Valkyrien Skies ships.");
        offsetX = file.getDouble("camera.shoulder.offsetX", -0.75D, -32.0D, 32.0D);
        offsetY = file.getDouble("camera.shoulder.offsetY", 0.0D, -32.0D, 32.0D);
        offsetZ = file.getDouble("camera.shoulder.offsetZ", 3.0D, -64.0D, 64.0D);
        minOffsetX = file.getDouble("camera.shoulder.minOffsetX", -3.0D, -64.0D, 64.0D);
        minOffsetY = file.getDouble("camera.shoulder.minOffsetY", -1.0D, -64.0D, 64.0D);
        minOffsetZ = file.getDouble("camera.shoulder.minOffsetZ", -3.0D, -64.0D, 64.0D);
        maxOffsetX = file.getDouble("camera.shoulder.maxOffsetX", 3.0D, -64.0D, 64.0D);
        maxOffsetY = file.getDouble("camera.shoulder.maxOffsetY", 1.5D, -64.0D, 64.0D);
        maxOffsetZ = file.getDouble("camera.shoulder.maxOffsetZ", 5.0D, -64.0D, 64.0D);
        unlimitedOffsetX = file.getBoolean("camera.shoulder.unlimitedOffsetX", false);
        unlimitedOffsetY = file.getBoolean("camera.shoulder.unlimitedOffsetY", false);
        unlimitedOffsetZ = file.getBoolean("camera.shoulder.unlimitedOffsetZ", false);
        cameraStepSize = file.getDouble("camera.shoulder.cameraStepSize", 0.025D, 0.0001D, 16.0D);
        transitionSpeed = file.getDouble("camera.shoulder.transitionSpeed", 0.25D, 0.05D, 1.0D);
        keepCameraOutOfHeadMultiplier = file.getDouble("camera.shoulder.keepCameraOutOfHeadMultiplier", 0.75D, 0.0D, 8.0D);
        dynamicallyAdjustOffsets = file.getBoolean("camera.shoulder.dynamicallyAdjustOffsets", true);
        limitPlayerReach = file.getBoolean("camera.shoulder.limitPlayerReach", true);
        useCustomRaytraceDistance = file.getBoolean("camera.shoulder.useCustomRaytraceDistance", true);
        customRaytraceDistance = file.getDouble("camera.shoulder.customRaytraceDistance", 400.0D, 0.0D, 4096.0D);
        hidePlayerWhenLookingUp = file.getBoolean("camera.shoulder.hidePlayerWhenLookingUp", true);
        hidePlayerWhenLookingUpAngle = file.getDouble("camera.shoulder.hidePlayerWhenLookingUpAngle", 15.0D, 0.0D, 90.0D);
        adaptiveHoldItems = file.getStringList("camera.shoulder.adaptiveHoldItems", Arrays.asList(
                "minecraft:snowball", "minecraft:egg", "minecraft:experience_bottle",
                "minecraft:ender_pearl", "minecraft:splash_potion", "minecraft:fishing_rod",
                "minecraft:lingering_potion"));
        adaptiveUseItems = file.getStringList("camera.shoulder.adaptiveUseItems", Collections.emptyList());
        adaptiveHoldProperties = file.getStringList("camera.shoulder.adaptiveHoldProperties",
                Collections.singletonList("minecraft:charged"));
        adaptiveUseProperties = file.getStringList("camera.shoulder.adaptiveUseProperties",
                Arrays.asList("minecraft:pull", "minecraft:throwing"));
        collision = file.getBoolean("camera.shoulder.collision", true);
        centerWhenClimbing = file.getBoolean("camera.shoulder.centerWhenClimbing", true);
        centerWhenLookingDownDegrees = file.getDouble(
                "camera.shoulder.centerWhenLookingDownDegrees", 15.0D, 0.0D, 90.0D);
        sprintXMultiplier = file.getDouble("camera.shoulder.sprintXMultiplier", 1.0D, 0.0D, 8.0D);
        sprintYMultiplier = file.getDouble("camera.shoulder.sprintYMultiplier", 1.0D, 0.0D, 8.0D);
        sprintZMultiplier = file.getDouble("camera.shoulder.sprintZMultiplier", 1.0D, 0.0D, 8.0D);
        passengerXMultiplier = file.getDouble("camera.shoulder.passengerXMultiplier", 1.0D, 0.0D, 8.0D);
        passengerYMultiplier = file.getDouble("camera.shoulder.passengerYMultiplier", 1.0D, 0.0D, 8.0D);
        passengerZMultiplier = file.getDouble("camera.shoulder.passengerZMultiplier", 1.0D, 0.0D, 8.0D);
        crosshairMode = normalizeCrosshairMode(file.getString("camera.shoulder.crosshairMode", "camera"));
        crosshairType = normalizeCrosshairType(file.getString("camera.shoulder.crosshairType", "dynamic"));
        crosshairVisibility.clear();
        crosshairVisibility.put(0, normalizeVisibility(file.getString("camera.shoulder.visibility.firstPerson", "always")));
        crosshairVisibility.put(1, normalizeVisibility(file.getString("camera.shoulder.visibility.thirdPersonBack", "never")));
        crosshairVisibility.put(2, normalizeVisibility(file.getString("camera.shoulder.visibility.thirdPersonFront", "never")));
        crosshairVisibility.put(3, normalizeVisibility(file.getString("camera.shoulder.visibility.shoulder", "always")));
        playerTransparency = file.getBoolean("camera.shoulder.playerTransparency", true);
        playerTransparencyPercent = file.getInt("camera.shoulder.playerTransparencyPercent", 100, 0, 100);
        valkyrienShipCollision = file.getBoolean("camera.shoulder.valkyrienShipCollision", false);
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("camera.shoulder.offsetX", offsetX)
                .set("camera.shoulder.offsetY", offsetY)
                .set("camera.shoulder.offsetZ", offsetZ)
                .set("camera.shoulder.minOffsetX", minOffsetX).set("camera.shoulder.minOffsetY", minOffsetY)
                .set("camera.shoulder.minOffsetZ", minOffsetZ).set("camera.shoulder.maxOffsetX", maxOffsetX)
                .set("camera.shoulder.maxOffsetY", maxOffsetY).set("camera.shoulder.maxOffsetZ", maxOffsetZ)
                .set("camera.shoulder.unlimitedOffsetX", unlimitedOffsetX).set("camera.shoulder.unlimitedOffsetY", unlimitedOffsetY)
                .set("camera.shoulder.unlimitedOffsetZ", unlimitedOffsetZ).set("camera.shoulder.cameraStepSize", cameraStepSize)
                .set("camera.shoulder.transitionSpeed", transitionSpeed)
                .set("camera.shoulder.keepCameraOutOfHeadMultiplier", keepCameraOutOfHeadMultiplier)
                .set("camera.shoulder.dynamicallyAdjustOffsets", dynamicallyAdjustOffsets)
                .set("camera.shoulder.limitPlayerReach", limitPlayerReach)
                .set("camera.shoulder.useCustomRaytraceDistance", useCustomRaytraceDistance)
                .set("camera.shoulder.customRaytraceDistance", customRaytraceDistance)
                .set("camera.shoulder.hidePlayerWhenLookingUp", hidePlayerWhenLookingUp)
                .set("camera.shoulder.hidePlayerWhenLookingUpAngle", hidePlayerWhenLookingUpAngle)
                .set("camera.shoulder.adaptiveHoldItems", adaptiveHoldItems)
                .set("camera.shoulder.adaptiveUseItems", adaptiveUseItems)
                .set("camera.shoulder.adaptiveHoldProperties", adaptiveHoldProperties)
                .set("camera.shoulder.adaptiveUseProperties", adaptiveUseProperties)
                .set("camera.shoulder.collision", collision)
                .set("camera.shoulder.centerWhenClimbing", centerWhenClimbing)
                .set("camera.shoulder.centerWhenLookingDownDegrees", centerWhenLookingDownDegrees)
                .set("camera.shoulder.sprintXMultiplier", sprintXMultiplier)
                .set("camera.shoulder.sprintYMultiplier", sprintYMultiplier)
                .set("camera.shoulder.sprintZMultiplier", sprintZMultiplier)
                .set("camera.shoulder.passengerXMultiplier", passengerXMultiplier)
                .set("camera.shoulder.passengerYMultiplier", passengerYMultiplier)
                .set("camera.shoulder.passengerZMultiplier", passengerZMultiplier)
                .set("camera.shoulder.crosshairMode", crosshairMode)
                .set("camera.shoulder.crosshairType", crosshairType)
                .set("camera.shoulder.visibility.firstPerson", visibility(0))
                .set("camera.shoulder.visibility.thirdPersonBack", visibility(1))
                .set("camera.shoulder.visibility.thirdPersonFront", visibility(2))
                .set("camera.shoulder.visibility.shoulder", visibility(3))
                .set("camera.shoulder.playerTransparency", playerTransparency)
                .set("camera.shoulder.playerTransparencyPercent", playerTransparencyPercent)
                .set("camera.shoulder.valkyrienShipCollision", valkyrienShipCollision)
                .save();
    }

    static String normalizeCrosshairMode(String value) {
        return "player".equals(value) || "dual".equals(value) || "off".equals(value)
                ? value : "camera";
    }

    static String normalizeCrosshairType(String value) {
        if (value == null) return "adaptive";
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        if ("dynamic".equals(normalized) || "static".equals(normalized)
                || "static_with_1pp".equals(normalized) || "dynamic_with_1pp".equals(normalized)) {
            return normalized;
        }
        return "adaptive";
    }

    static String normalizeVisibility(String value) {
        if (value == null) return "always";
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        switch (normalized) {
            case "never": case "when_aiming": case "when_in_range":
            case "when_aiming_or_in_range": return normalized;
            default: return "always";
        }
    }

    private static String visibility(int perspective) {
        return crosshairVisibility.getOrDefault(perspective, "always");
    }

    static ShoulderCrosshairType crosshairType(boolean aiming) {
        String value = crosshairType;
        if ("dynamic".equals(value)) return ShoulderCrosshairType.DYNAMIC;
        if ("static".equals(value)) return ShoulderCrosshairType.STATIC;
        if ("static_with_1pp".equals(value)) return ShoulderCrosshairType.STATIC_WITH_1PP;
        if ("dynamic_with_1pp".equals(value)) return ShoulderCrosshairType.DYNAMIC_WITH_1PP;
        return ShoulderCrosshairType.ADAPTIVE;
    }

    static ShoulderCrosshairVisibility visibilityRule(int perspective) {
        String value = visibility(perspective);
        if ("never".equals(value)) return ShoulderCrosshairVisibility.NEVER;
        if ("when_aiming".equals(value)) return ShoulderCrosshairVisibility.WHEN_AIMING;
        if ("when_in_range".equals(value)) return ShoulderCrosshairVisibility.WHEN_IN_RANGE;
        if ("when_aiming_or_in_range".equals(value)) return ShoulderCrosshairVisibility.WHEN_AIMING_OR_IN_RANGE;
        return ShoulderCrosshairVisibility.ALWAYS;
    }

    static void adjustX(double delta) {
        offsetX = clamp(offsetX + delta, minOffsetX, maxOffsetX, unlimitedOffsetX);
        save();
    }
    static void adjustY(double delta) {
        offsetY = clamp(offsetY + delta, minOffsetY, maxOffsetY, unlimitedOffsetY);
        save();
    }
    static void adjustZ(double delta) {
        offsetZ = clamp(offsetZ + delta, minOffsetZ, maxOffsetZ, unlimitedOffsetZ);
        save();
    }
    private static double clamp(double value, double min, double max, boolean unlimited) {
        return unlimited ? value : Math.max(Math.min(min, max), Math.min(Math.max(min, max), value));
    }

}
