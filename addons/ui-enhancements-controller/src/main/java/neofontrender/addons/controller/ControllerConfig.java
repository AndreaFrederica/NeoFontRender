package neofontrender.addons.controller;

import neofontrender.api.config.NfrConfigApi;
import neofontrender.api.config.NfrConfigFile;
import neofontrender.api.config.NfrConfigStorage;

/** Independent runtime configuration for the optional controller addon. */
public final class ControllerConfig {
    public static final float DEFAULT_DEADZONE = 0.15F;
    public static final float DEFAULT_LOOK_SENSITIVITY = 1.0F;
    public static final float DEFAULT_FLIGHT_SENSITIVITY = 1.0F;
    public static final float DEFAULT_CURSOR_SENSITIVITY = 1.0F;
    public static final float DEFAULT_CURSOR_BASE_SPEED = 120.0F;
    public static final float DEFAULT_CURSOR_MAX_SPEED = 360.0F;
    public static final float DEFAULT_CURSOR_ACCELERATION = 600.0F;
    public static final float DEFAULT_CURSOR_SMOOTHING = 0.35F;

    private static NfrConfigFile file;
    private static float deadzone = DEFAULT_DEADZONE;
    private static float lookSensitivity = DEFAULT_LOOK_SENSITIVITY;
    private static float flightSensitivity = DEFAULT_FLIGHT_SENSITIVITY;
    private static float cursorSensitivity = DEFAULT_CURSOR_SENSITIVITY;
    private static float cursorBaseSpeed = DEFAULT_CURSOR_BASE_SPEED;
    private static float cursorMaxSpeed = DEFAULT_CURSOR_MAX_SPEED;
    private static float cursorAcceleration = DEFAULT_CURSOR_ACCELERATION;
    private static float cursorSmoothing = DEFAULT_CURSOR_SMOOTHING;
    private static boolean invertLookX;
    private static boolean invertLookY;
    private static boolean invertFlightPitch;
    private static boolean invertFlightYaw;
    private static boolean invertFlightRoll;
    private static boolean vibrationEnabled = true;
    private static boolean slotSnapping = true;
    private static String selectedDeviceKey = "";

    private ControllerConfig() {}

    public static synchronized void load() {
        NfrConfigFile config = file();
        config.define("deadzone", (double) DEFAULT_DEADZONE,
                        "Dead-zone applied to controller axes before UIE routing.")
                .define("lookSensitivity", (double) DEFAULT_LOOK_SENSITIVITY,
                        "Right-stick sensitivity for camera look actions.")
                .define("flightSensitivity", (double) DEFAULT_FLIGHT_SENSITIVITY,
                        "Stick sensitivity for flight pitch, yaw, and roll actions.")
                .define("cursorSensitivity", (double) DEFAULT_CURSOR_SENSITIVITY,
                        "Left-stick sensitivity for the GUI virtual cursor.")
                .define("cursorBaseSpeed", (double) DEFAULT_CURSOR_BASE_SPEED,
                        "Initial GUI virtual cursor speed in scaled pixels per second.")
                .define("cursorMaxSpeed", (double) DEFAULT_CURSOR_MAX_SPEED,
                        "Maximum GUI virtual cursor speed in scaled pixels per second.")
                .define("cursorAcceleration", (double) DEFAULT_CURSOR_ACCELERATION,
                        "GUI virtual cursor acceleration in scaled pixels per second squared.")
                .define("cursorSmoothing", (double) DEFAULT_CURSOR_SMOOTHING,
                        "Temporal smoothing applied to the GUI virtual cursor stick vector.")
                .define("invertLookX", false, "Invert the horizontal camera-look axis.")
                .define("invertLookY", false, "Invert the vertical camera-look axis.")
                .define("invertFlightPitch", false, "Invert the flight pitch axis.")
                .define("invertFlightYaw", false, "Invert the flight yaw axis.")
                .define("invertFlightRoll", false, "Invert the flight roll axis.")
                .define("vibrationEnabled", true,
                        "Allow controller vibration when a UIE haptics output is available.")
                .define("slotSnapping", true,
                        "Attract the virtual cursor to nearby occupied container slots.")
                .define("selectedDevice", "",
                        "Stable SDL identity of the controller selected for UI input.")
                .define("bindings", ControllerBindings.defaultRecords(),
                        "Physical SDL controls assigned to UIE logical actions.")
                .define("forgeKeyBindings", java.util.Collections.emptyList(),
                        "Physical SDL controls assigned to Minecraft key bindings "
                                + "(vanilla defaults and Forge/mod registrations).");
        deadzone = (float) config.getDouble("deadzone", DEFAULT_DEADZONE, 0.0D, 0.5D);
        lookSensitivity = (float) config.getDouble(
                "lookSensitivity", DEFAULT_LOOK_SENSITIVITY, 0.1D, 4.0D);
        flightSensitivity = (float) config.getDouble(
                "flightSensitivity", DEFAULT_FLIGHT_SENSITIVITY, 0.1D, 4.0D);
        cursorSensitivity = (float) config.getDouble(
                "cursorSensitivity", DEFAULT_CURSOR_SENSITIVITY, 0.25D, 3.0D);
        cursorBaseSpeed = (float) config.getDouble(
                "cursorBaseSpeed", DEFAULT_CURSOR_BASE_SPEED, 20.0D, 300.0D);
        cursorMaxSpeed = (float) config.getDouble(
                "cursorMaxSpeed", DEFAULT_CURSOR_MAX_SPEED, 60.0D, 720.0D);
        cursorAcceleration = (float) config.getDouble(
                "cursorAcceleration", DEFAULT_CURSOR_ACCELERATION, 0.0D, 2_000.0D);
        cursorSmoothing = (float) config.getDouble(
                "cursorSmoothing", DEFAULT_CURSOR_SMOOTHING, 0.0D, 1.0D);
        invertLookX = config.getBoolean("invertLookX", false);
        invertLookY = config.getBoolean("invertLookY", false);
        invertFlightPitch = config.getBoolean("invertFlightPitch", false);
        invertFlightYaw = config.getBoolean("invertFlightYaw", false);
        invertFlightRoll = config.getBoolean("invertFlightRoll", false);
        vibrationEnabled = config.getBoolean("vibrationEnabled", true);
        slotSnapping = config.getBoolean("slotSnapping", true);
        selectedDeviceKey = config.getString("selectedDevice", "").trim();
        ControllerBindings.load(config.getStringList(
                "bindings", ControllerBindings.defaultRecords()));
        ControllerForgeBindings.load(config.getStringList(
                "forgeKeyBindings", java.util.Collections.emptyList()));
        config.save();
    }

    public static synchronized void save() {
        file().set("deadzone", (double) deadzone)
                .set("lookSensitivity", (double) lookSensitivity)
                .set("flightSensitivity", (double) flightSensitivity)
                .set("cursorSensitivity", (double) cursorSensitivity)
                .set("cursorBaseSpeed", (double) cursorBaseSpeed)
                .set("cursorMaxSpeed", (double) cursorMaxSpeed)
                .set("cursorAcceleration", (double) cursorAcceleration)
                .set("cursorSmoothing", (double) cursorSmoothing)
                .set("invertLookX", invertLookX)
                .set("invertLookY", invertLookY)
                .set("invertFlightPitch", invertFlightPitch)
                .set("invertFlightYaw", invertFlightYaw)
                .set("invertFlightRoll", invertFlightRoll)
                .set("vibrationEnabled", vibrationEnabled)
                .set("slotSnapping", slotSnapping)
                .set("selectedDevice", selectedDeviceKey)
                .set("bindings", ControllerBindings.serialize())
                .set("forgeKeyBindings", ControllerForgeBindings.serialize())
                .save();
    }

    public static synchronized float deadzone() { return deadzone; }
    public static synchronized float lookSensitivity() { return lookSensitivity; }
    public static synchronized float flightSensitivity() { return flightSensitivity; }
    public static synchronized float cursorSensitivity() { return cursorSensitivity; }
    public static synchronized float cursorBaseSpeed() { return cursorBaseSpeed; }
    public static synchronized float cursorMaxSpeed() { return cursorMaxSpeed; }
    public static synchronized float cursorAcceleration() { return cursorAcceleration; }
    public static synchronized float cursorSmoothing() { return cursorSmoothing; }
    public static synchronized boolean invertLookX() { return invertLookX; }
    public static synchronized boolean invertLookY() { return invertLookY; }
    public static synchronized boolean invertFlightPitch() { return invertFlightPitch; }
    public static synchronized boolean invertFlightYaw() { return invertFlightYaw; }
    public static synchronized boolean invertFlightRoll() { return invertFlightRoll; }
    public static synchronized boolean vibrationEnabled() { return vibrationEnabled; }
    public static synchronized boolean slotSnapping() { return slotSnapping; }
    public static synchronized String selectedDeviceKey() { return selectedDeviceKey; }

    public static synchronized void setDeadzone(float value) {
        deadzone = clamp(value, 0.0F, 0.5F, DEFAULT_DEADZONE);
    }

    public static synchronized void setLookSensitivity(float value) {
        lookSensitivity = clamp(value, 0.1F, 4.0F, DEFAULT_LOOK_SENSITIVITY);
    }

    public static synchronized void setFlightSensitivity(float value) {
        flightSensitivity = clamp(value, 0.1F, 4.0F, DEFAULT_FLIGHT_SENSITIVITY);
    }

    public static synchronized void setCursorSensitivity(float value) {
        cursorSensitivity = clamp(value, 0.25F, 3.0F, DEFAULT_CURSOR_SENSITIVITY);
    }

    public static synchronized void setCursorBaseSpeed(float value) {
        cursorBaseSpeed = clamp(value, 20.0F, 300.0F, DEFAULT_CURSOR_BASE_SPEED);
    }

    public static synchronized void setCursorMaxSpeed(float value) {
        cursorMaxSpeed = clamp(value, 60.0F, 720.0F, DEFAULT_CURSOR_MAX_SPEED);
    }

    public static synchronized void setCursorAcceleration(float value) {
        cursorAcceleration = clamp(value, 0.0F, 2_000.0F, DEFAULT_CURSOR_ACCELERATION);
    }

    public static synchronized void setCursorSmoothing(float value) {
        cursorSmoothing = clamp(value, 0.0F, 1.0F, DEFAULT_CURSOR_SMOOTHING);
    }

    public static synchronized void setInvertLookX(boolean value) { invertLookX = value; }
    public static synchronized void setInvertLookY(boolean value) { invertLookY = value; }
    public static synchronized void setInvertFlightPitch(boolean value) { invertFlightPitch = value; }
    public static synchronized void setInvertFlightYaw(boolean value) { invertFlightYaw = value; }
    public static synchronized void setInvertFlightRoll(boolean value) { invertFlightRoll = value; }
    public static synchronized void setVibrationEnabled(boolean value) { vibrationEnabled = value; }
    public static synchronized void setSlotSnapping(boolean value) { slotSnapping = value; }
    public static synchronized void setSelectedDeviceKey(String value) {
        selectedDeviceKey = value == null ? "" : value.trim();
    }

    private static NfrConfigFile file() {
        if (file == null) {
            file = NfrConfigApi.builder(ControllerAddonMod.MOD_ID)
                    .storage(NfrConfigStorage.INDEPENDENT)
                    .fileName("neofontrender-ui-enhancements-controller.toml")
                    .open();
        }
        return file;
    }

    private static float clamp(float value, float min, float max, float fallback) {
        if (!Float.isFinite(value)) value = fallback;
        return Math.max(min, Math.min(max, value));
    }
}
