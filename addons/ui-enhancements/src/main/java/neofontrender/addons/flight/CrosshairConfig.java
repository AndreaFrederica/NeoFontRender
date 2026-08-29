package neofontrender.addons.flight;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

import java.util.Locale;

/** CCM-compatible crosshair configuration adapted to Minecraft 1.12.2. */
final class CrosshairConfig {
    static boolean customEnabled;
    static boolean preferModCrosshair = true;
    static boolean hideVanillaDuringFlightHud = true;
    static boolean hideForgeLayerDuringFlightHud;

    static String style = "cross";
    static boolean keepDebugCrosshair;
    static int color = 0xFFFFFFFF;
    static boolean adaptiveColor;
    static int width = 5;
    static int height = 5;
    static int gap = 3;
    static int thickness = 1;
    static int rotation;
    static int scalePercent = 100;
    static int offsetX;
    static int offsetY;

    static boolean visibleByDefault = true;
    static boolean visibleWithHiddenGui = true;
    static boolean visibleInDebug = true;
    static boolean visibleInThirdPerson;
    static boolean visibleAsSpectator = true;
    static boolean visibleHoldingRanged = true;
    static boolean visibleHoldingThrowable = true;
    static boolean visibleUsingSpyglass;

    static boolean outlineEnabled = true;
    static int outlineColor = 0xFF000000;
    static boolean dotEnabled;
    static int dotColor = 0xFFFFFFFF;

    static boolean dynamicAttack = true;
    static boolean dynamicBow = true;
    static boolean highlightHostiles = true;
    static boolean highlightPassives = true;
    static boolean highlightPlayers = true;
    static int hostileColor = 0xFFDC2828;
    static int passiveColor = 0xFF28E628;
    static int playerColor = 0xFF3C3CF0;

    static boolean itemCooldownEnabled = true;
    static int itemCooldownColor = 0x50FFFFFF;
    static boolean rainbowEnabled;
    static int rainbowSpeed = 500;
    static boolean toolDamageEnabled = true;
    static boolean projectileIndicatorEnabled = true;
    static boolean blockInteractionFirstPerson;
    static boolean blockInteractionThirdPerson;
    static boolean blockInteractionShoulder;
    static boolean blockInteractionFreeLook;
    static boolean blockInteractionCursorLook = true;
    static boolean blockInteractionDrone;

    static int drawnSize = 15;
    static String drawnPixels = "";
    static String compatSpyglassItems = "";
    static String compatCrossbowItems = "";
    static String compatTridentItems = "";
    static String compatRangedItems = "";

    private CrosshairConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("crosshair.customEnabled", false,
                        "Enable UIE's custom crosshair renderer.")
                .define("crosshair.preferModCrosshair", true,
                        "Allow item-specific mod crosshairs such as TiC's to take priority; UIE remains the fallback.")
                .define("crosshair.hideVanillaDuringFlightHud", true,
                        "Allow themes using HIDE_VANILLA to suppress Minecraft's vanilla/custom crosshair.")
                .define("crosshair.hideForgeLayerDuringFlightHud", false,
                        "Cancel Forge's CROSSHAIRS layer too; this can also hide mod crosshairs.")
                .define("crosshair.style", "cross", "vanilla, vanilla_plus, cross, dot, circle, square, triangle, arrow, debug, drawn, or chevron")
                .define("crosshair.keepDebugCrosshair", false, "Use the vanilla 3D debug crosshair while F3 is visible.")
                .define("crosshair.color", "#FFFFFFFF", "Custom crosshair ARGB color.")
                .define("crosshair.adaptiveColor", false, "Use Minecraft's contrast/invert blending for the main shape.")
                .define("crosshair.width", 5, "Horizontal shape size in pixels.")
                .define("crosshair.height", 5, "Vertical shape size in pixels.")
                .define("crosshair.gap", 3, "Center gap in pixels at 100% scale.")
                .define("crosshair.thickness", 1, "Line thickness in pixels.")
                .define("crosshair.rotation", 0, "Clockwise rotation in degrees.")
                .define("crosshair.scalePercent", 100, "Custom crosshair scale percentage.")
                .define("crosshair.offsetX", 0, "Horizontal screen offset in scaled pixels.")
                .define("crosshair.offsetY", 0, "Vertical screen offset in scaled pixels.")
                .define("crosshair.visible.default", true, "Master visibility for the custom crosshair.")
                .define("crosshair.visible.hiddenGui", true, "Keep the crosshair when the vanilla HUD is hidden, where supported by Forge.")
                .define("crosshair.visible.debug", true, "Show while the F3 debug HUD is visible.")
                .define("crosshair.visible.thirdPerson", false, "Show in ordinary third-person views; Shoulder Surfing is treated separately.")
                .define("crosshair.visible.spectator", true, "Show in spectator mode.")
                .define("crosshair.visible.holdingRanged", true, "Show while holding a bow.")
                .define("crosshair.visible.holdingThrowable", true, "Show while holding a throwable item.")
                .define("crosshair.visible.usingSpyglass", false,
                        "Show while using a backported spyglass/telescope or UIE's C-key zoom.")
                .define("crosshair.outline.enabled", true, "Draw a contrasting outline around supported shapes.")
                .define("crosshair.outline.color", "#FF000000", "Outline ARGB color.")
                .define("crosshair.dot.enabled", false, "Draw a center dot in addition to the selected style.")
                .define("crosshair.dot.color", "#FFFFFFFF", "Center-dot ARGB color.")
                .define("crosshair.dynamic.attack", true, "Expand the gap while the attack cooldown recovers.")
                .define("crosshair.dynamic.bow", true, "Contract the gap as a bow is drawn.")
                .define("crosshair.highlight.hostiles", true, "Color the crosshair when targeting hostile mobs.")
                .define("crosshair.highlight.passives", true, "Color the crosshair when targeting passive mobs.")
                .define("crosshair.highlight.players", true, "Color the crosshair when targeting players.")
                .define("crosshair.highlight.hostileColor", "#FFDC2828", "Hostile target ARGB color.")
                .define("crosshair.highlight.passiveColor", "#FF28E628", "Passive target ARGB color.")
                .define("crosshair.highlight.playerColor", "#FF3C3CF0", "Player target ARGB color.")
                .define("crosshair.itemCooldown.enabled", true, "Draw cooldown rings for pearls and chorus fruit.")
                .define("crosshair.itemCooldown.color", "#50FFFFFF", "Item cooldown ring ARGB color.")
                .define("crosshair.rainbow.enabled", false, "Animate the main color through a rainbow.")
                .define("crosshair.rainbow.speed", 500, "Rainbow animation speed from 0 to 1000.")
                .define("crosshair.indicator.toolDamage", true, "Show remaining durability when it reaches ten or less.")
                .define("crosshair.indicator.projectiles", true, "Show the selected bow ammunition and count.")
                .define("crosshair.indicator.blockInteraction.firstPerson", false,
                        "Show a block-interaction marker in first person.")
                .define("crosshair.indicator.blockInteraction.thirdPerson", false,
                        "Show a block-interaction marker in ordinary third person.")
                .define("crosshair.indicator.blockInteraction.shoulder", false,
                        "Show a block-interaction marker in Shoulder view.")
                .define("crosshair.indicator.blockInteraction.freeLook", false,
                        "Show a block-interaction marker in Free Look.")
                .define("crosshair.indicator.blockInteraction.cursorLook", true,
                        "Show a block-interaction marker in Cursor Look.")
                .define("crosshair.indicator.blockInteraction.drone", false,
                        "Show a block-interaction marker in Drone view.")
                .define("crosshair.drawn.size", 15, "Editable drawn-crosshair canvas size.")
                .define("crosshair.drawn.pixels", "", "Semicolon-separated x,y coordinates for the drawn style.")
                .define("crosshair.compat.spyglassItems", "",
                        "Additional exact spyglass item IDs, separated by commas; bundled TOML defaults remain active.")
                .define("crosshair.compat.crossbowItems", "",
                        "Additional exact crossbow item IDs, separated by commas; bundled TOML defaults remain active.")
                .define("crosshair.compat.tridentItems", "",
                        "Additional exact trident item IDs, separated by commas; bundled TOML defaults remain active.")
                .define("crosshair.compat.rangedItems", "",
                        "Additional exact non-charging ranged weapon IDs, separated by commas; bundled TOML defaults remain active.")
                // Kept so existing UIE 0.3 configs migrate without changing their appearance.
                .define("crosshair.armLength", 5, "Legacy arm length; used as the initial width and height.");

        customEnabled = file.getBoolean("crosshair.customEnabled", false);
        preferModCrosshair = file.getBoolean("crosshair.preferModCrosshair", true);
        hideVanillaDuringFlightHud = file.getBoolean("crosshair.hideVanillaDuringFlightHud", true);
        hideForgeLayerDuringFlightHud = file.getBoolean("crosshair.hideForgeLayerDuringFlightHud", false);
        style = normalizeStyle(file.getString("crosshair.style", "cross"));
        keepDebugCrosshair = file.getBoolean("crosshair.keepDebugCrosshair", false);
        color = parseColor(file.getString("crosshair.color", "#FFFFFFFF"), 0xFFFFFFFF);
        adaptiveColor = file.getBoolean("crosshair.adaptiveColor", false);
        int legacyArm = file.getInt("crosshair.armLength", 5, 1, 24);
        width = file.getInt("crosshair.width", legacyArm, 0, 50);
        height = file.getInt("crosshair.height", legacyArm, 0, 50);
        gap = file.getInt("crosshair.gap", 3, 0, 50);
        thickness = file.getInt("crosshair.thickness", 1, 1, 10);
        rotation = file.getInt("crosshair.rotation", 0, 0, 360);
        scalePercent = file.getInt("crosshair.scalePercent", 100, 25, 500);
        offsetX = file.getInt("crosshair.offsetX", 0, -500, 500);
        offsetY = file.getInt("crosshair.offsetY", 0, -500, 500);

        visibleByDefault = file.getBoolean("crosshair.visible.default", true);
        visibleWithHiddenGui = file.getBoolean("crosshair.visible.hiddenGui", true);
        visibleInDebug = file.getBoolean("crosshair.visible.debug", true);
        visibleInThirdPerson = file.getBoolean("crosshair.visible.thirdPerson", false);
        visibleAsSpectator = file.getBoolean("crosshair.visible.spectator", true);
        visibleHoldingRanged = file.getBoolean("crosshair.visible.holdingRanged", true);
        visibleHoldingThrowable = file.getBoolean("crosshair.visible.holdingThrowable", true);
        visibleUsingSpyglass = file.getBoolean("crosshair.visible.usingSpyglass", true);

        outlineEnabled = file.getBoolean("crosshair.outline.enabled", true);
        outlineColor = parseColor(file.getString("crosshair.outline.color", "#FF000000"), 0xFF000000);
        dotEnabled = file.getBoolean("crosshair.dot.enabled", false);
        dotColor = parseColor(file.getString("crosshair.dot.color", "#FFFFFFFF"), 0xFFFFFFFF);
        dynamicAttack = file.getBoolean("crosshair.dynamic.attack", true);
        dynamicBow = file.getBoolean("crosshair.dynamic.bow", true);
        highlightHostiles = file.getBoolean("crosshair.highlight.hostiles", true);
        highlightPassives = file.getBoolean("crosshair.highlight.passives", true);
        highlightPlayers = file.getBoolean("crosshair.highlight.players", true);
        hostileColor = parseColor(file.getString("crosshair.highlight.hostileColor", "#FFDC2828"), 0xFFDC2828);
        passiveColor = parseColor(file.getString("crosshair.highlight.passiveColor", "#FF28E628"), 0xFF28E628);
        playerColor = parseColor(file.getString("crosshair.highlight.playerColor", "#FF3C3CF0"), 0xFF3C3CF0);
        itemCooldownEnabled = file.getBoolean("crosshair.itemCooldown.enabled", true);
        itemCooldownColor = parseColor(file.getString("crosshair.itemCooldown.color", "#50FFFFFF"), 0x50FFFFFF);
        rainbowEnabled = file.getBoolean("crosshair.rainbow.enabled", false);
        rainbowSpeed = file.getInt("crosshair.rainbow.speed", 500, 0, 1000);
        toolDamageEnabled = file.getBoolean("crosshair.indicator.toolDamage", true);
        projectileIndicatorEnabled = file.getBoolean("crosshair.indicator.projectiles", true);
        blockInteractionFirstPerson = file.getBoolean(
                "crosshair.indicator.blockInteraction.firstPerson", false);
        blockInteractionThirdPerson = file.getBoolean(
                "crosshair.indicator.blockInteraction.thirdPerson", false);
        blockInteractionShoulder = file.getBoolean(
                "crosshair.indicator.blockInteraction.shoulder", false);
        blockInteractionFreeLook = file.getBoolean(
                "crosshair.indicator.blockInteraction.freeLook", false);
        blockInteractionCursorLook = file.getBoolean(
                "crosshair.indicator.blockInteraction.cursorLook", true);
        blockInteractionDrone = file.getBoolean(
                "crosshair.indicator.blockInteraction.drone", false);
        drawnSize = file.getInt("crosshair.drawn.size", 15, 7, 57);
        drawnPixels = file.getString("crosshair.drawn.pixels", "");
        compatSpyglassItems = file.getString("crosshair.compat.spyglassItems", "");
        compatCrossbowItems = file.getString("crosshair.compat.crossbowItems", "");
        compatTridentItems = file.getString("crosshair.compat.tridentItems", "");
        compatRangedItems = file.getString("crosshair.compat.rangedItems", "");
        CrosshairItemCompat.configure(compatSpyglassItems, compatCrossbowItems,
                compatTridentItems, compatRangedItems);
        file.save();
    }

    static void save() {
        style = normalizeStyle(style);
        width = clamp(width, 0, 50); height = clamp(height, 0, 50);
        gap = clamp(gap, 0, 50); thickness = clamp(thickness, 1, 10);
        rotation = clamp(rotation, 0, 360); scalePercent = clamp(scalePercent, 25, 500);
        offsetX = clamp(offsetX, -500, 500); offsetY = clamp(offsetY, -500, 500);
        rainbowSpeed = clamp(rainbowSpeed, 0, 1000); drawnSize = clamp(drawnSize, 7, 57);
        UiEnhancementsConfig.file()
                .set("crosshair.customEnabled", customEnabled)
                .set("crosshair.preferModCrosshair", preferModCrosshair)
                .set("crosshair.hideVanillaDuringFlightHud", hideVanillaDuringFlightHud)
                .set("crosshair.hideForgeLayerDuringFlightHud", hideForgeLayerDuringFlightHud)
                .set("crosshair.style", style).set("crosshair.keepDebugCrosshair", keepDebugCrosshair)
                .set("crosshair.color", hex(color)).set("crosshair.adaptiveColor", adaptiveColor)
                .set("crosshair.width", width).set("crosshair.height", height).set("crosshair.gap", gap)
                .set("crosshair.thickness", thickness).set("crosshair.rotation", rotation)
                .set("crosshair.scalePercent", scalePercent).set("crosshair.offsetX", offsetX).set("crosshair.offsetY", offsetY)
                .set("crosshair.visible.default", visibleByDefault).set("crosshair.visible.hiddenGui", visibleWithHiddenGui)
                .set("crosshair.visible.debug", visibleInDebug).set("crosshair.visible.thirdPerson", visibleInThirdPerson)
                .set("crosshair.visible.spectator", visibleAsSpectator).set("crosshair.visible.holdingRanged", visibleHoldingRanged)
                .set("crosshair.visible.holdingThrowable", visibleHoldingThrowable).set("crosshair.visible.usingSpyglass", visibleUsingSpyglass)
                .set("crosshair.outline.enabled", outlineEnabled).set("crosshair.outline.color", hex(outlineColor))
                .set("crosshair.dot.enabled", dotEnabled).set("crosshair.dot.color", hex(dotColor))
                .set("crosshair.dynamic.attack", dynamicAttack).set("crosshair.dynamic.bow", dynamicBow)
                .set("crosshair.highlight.hostiles", highlightHostiles).set("crosshair.highlight.passives", highlightPassives)
                .set("crosshair.highlight.players", highlightPlayers).set("crosshair.highlight.hostileColor", hex(hostileColor))
                .set("crosshair.highlight.passiveColor", hex(passiveColor)).set("crosshair.highlight.playerColor", hex(playerColor))
                .set("crosshair.itemCooldown.enabled", itemCooldownEnabled).set("crosshair.itemCooldown.color", hex(itemCooldownColor))
                .set("crosshair.rainbow.enabled", rainbowEnabled).set("crosshair.rainbow.speed", rainbowSpeed)
                .set("crosshair.indicator.toolDamage", toolDamageEnabled)
                .set("crosshair.indicator.projectiles", projectileIndicatorEnabled)
                .set("crosshair.indicator.blockInteraction.firstPerson", blockInteractionFirstPerson)
                .set("crosshair.indicator.blockInteraction.thirdPerson", blockInteractionThirdPerson)
                .set("crosshair.indicator.blockInteraction.shoulder", blockInteractionShoulder)
                .set("crosshair.indicator.blockInteraction.freeLook", blockInteractionFreeLook)
                .set("crosshair.indicator.blockInteraction.cursorLook", blockInteractionCursorLook)
                .set("crosshair.indicator.blockInteraction.drone", blockInteractionDrone)
                .set("crosshair.drawn.size", drawnSize).set("crosshair.drawn.pixels", drawnPixels)
                .set("crosshair.compat.spyglassItems", compatSpyglassItems)
                .set("crosshair.compat.crossbowItems", compatCrossbowItems)
                .set("crosshair.compat.tridentItems", compatTridentItems)
                .set("crosshair.compat.rangedItems", compatRangedItems)
                .save();
        CrosshairItemCompat.configure(compatSpyglassItems, compatCrossbowItems,
                compatTridentItems, compatRangedItems);
    }

    static Snapshot snapshot() { return new Snapshot(); }

    static final class Snapshot {
        private final boolean[] booleans = {customEnabled, preferModCrosshair,
                hideVanillaDuringFlightHud, hideForgeLayerDuringFlightHud,
                keepDebugCrosshair, adaptiveColor, visibleByDefault, visibleWithHiddenGui, visibleInDebug,
                visibleInThirdPerson, visibleAsSpectator, visibleHoldingRanged, visibleHoldingThrowable,
                visibleUsingSpyglass, outlineEnabled, dotEnabled, dynamicAttack, dynamicBow, highlightHostiles, highlightPassives,
                highlightPlayers, itemCooldownEnabled, rainbowEnabled, toolDamageEnabled,
                projectileIndicatorEnabled, blockInteractionFirstPerson,
                blockInteractionThirdPerson, blockInteractionShoulder,
                blockInteractionFreeLook, blockInteractionCursorLook, blockInteractionDrone};
        private final int[] ints = {color, width, height, gap, thickness, rotation, scalePercent, offsetX, offsetY,
                outlineColor, dotColor, hostileColor, passiveColor, playerColor, itemCooldownColor, rainbowSpeed, drawnSize};
        private final String savedStyle = style;
        private final String savedPixels = drawnPixels;
        private final String savedSpyglassItems = compatSpyglassItems;
        private final String savedCrossbowItems = compatCrossbowItems;
        private final String savedTridentItems = compatTridentItems;
        private final String savedRangedItems = compatRangedItems;

        void restore() {
            int b = 0;
            customEnabled = booleans[b++]; preferModCrosshair = booleans[b++];
            hideVanillaDuringFlightHud = booleans[b++]; hideForgeLayerDuringFlightHud = booleans[b++];
            keepDebugCrosshair = booleans[b++]; adaptiveColor = booleans[b++]; visibleByDefault = booleans[b++];
            visibleWithHiddenGui = booleans[b++]; visibleInDebug = booleans[b++]; visibleInThirdPerson = booleans[b++];
            visibleAsSpectator = booleans[b++]; visibleHoldingRanged = booleans[b++]; visibleHoldingThrowable = booleans[b++];
            visibleUsingSpyglass = booleans[b++]; outlineEnabled = booleans[b++]; dotEnabled = booleans[b++]; dynamicAttack = booleans[b++]; dynamicBow = booleans[b++];
            highlightHostiles = booleans[b++]; highlightPassives = booleans[b++]; highlightPlayers = booleans[b++];
            itemCooldownEnabled = booleans[b++]; rainbowEnabled = booleans[b++]; toolDamageEnabled = booleans[b++];
            projectileIndicatorEnabled = booleans[b++];
            blockInteractionFirstPerson = booleans[b++];
            blockInteractionThirdPerson = booleans[b++];
            blockInteractionShoulder = booleans[b++];
            blockInteractionFreeLook = booleans[b++];
            blockInteractionCursorLook = booleans[b++];
            blockInteractionDrone = booleans[b];
            int i = 0;
            color = ints[i++]; width = ints[i++]; height = ints[i++]; gap = ints[i++]; thickness = ints[i++];
            rotation = ints[i++]; scalePercent = ints[i++]; offsetX = ints[i++]; offsetY = ints[i++]; outlineColor = ints[i++];
            dotColor = ints[i++]; hostileColor = ints[i++]; passiveColor = ints[i++]; playerColor = ints[i++];
            itemCooldownColor = ints[i++]; rainbowSpeed = ints[i++]; drawnSize = ints[i];
            style = savedStyle; drawnPixels = savedPixels;
            compatSpyglassItems = savedSpyglassItems; compatCrossbowItems = savedCrossbowItems;
            compatTridentItems = savedTridentItems; compatRangedItems = savedRangedItems;
            CrosshairItemCompat.configure(compatSpyglassItems, compatCrossbowItems,
                    compatTridentItems, compatRangedItems);
        }
    }

    private static String normalizeStyle(String value) {
        String normalized = value == null ? "cross" : value.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "vanilla": case "vanilla_plus": case "cross": case "dot": case "circle": case "square":
            case "triangle": case "arrow": case "debug": case "drawn": case "chevron": return normalized;
            default: return "cross";
        }
    }

    private static int parseColor(String value, int fallback) {
        try {
            String normalized = value == null ? "" : value.trim();
            if (normalized.startsWith("#")) normalized = normalized.substring(1);
            else if (normalized.startsWith("0x") || normalized.startsWith("0X")) normalized = normalized.substring(2);
            long parsed = Long.parseLong(normalized, 16);
            if (normalized.length() <= 6) parsed |= 0xFF000000L;
            return (int) parsed;
        } catch (RuntimeException ignored) { return fallback; }
    }

    private static String hex(int color) { return String.format(Locale.ROOT, "#%08X", color); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
