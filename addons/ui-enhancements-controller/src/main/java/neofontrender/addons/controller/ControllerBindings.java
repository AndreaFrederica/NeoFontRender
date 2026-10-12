package neofontrender.addons.controller;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputBinding;
import neofontrender.addons.api.input.InputValue;
import neofontrender.addons.controller.sdl.AxisNormalizer;
import neofontrender.addons.controller.sdl.ControllerControls;
import neofontrender.addons.controller.sdl.ControllerSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mutable controller profile with deterministic defaults and TOML-safe serialization. */
public final class ControllerBindings {
    private static final List<ControllerBindingSpec> DEFAULTS = createDefaults();
    private static List<ControllerBindingSpec> current = copy(DEFAULTS);

    private ControllerBindings() {}

    public static synchronized List<ControllerBindingSpec> all() {
        return Collections.unmodifiableList(copy(current));
    }

    public static synchronized List<ControllerBindingSpec> snapshot() { return copy(current); }

    public static synchronized void restore(List<ControllerBindingSpec> values) {
        current = sanitize(values);
    }

    public static synchronized void assign(String key, ResourceLocation control) {
        for (int index = 0; index < current.size(); index++) {
            ControllerBindingSpec spec = current.get(index);
            if (spec.key().equals(key)) {
                current.set(index, spec.withControl(control));
                return;
            }
        }
    }

    public static synchronized void resetDefaults() { current = copy(DEFAULTS); }

    public static synchronized int uses(ResourceLocation control) {
        if (control == null) return 0;
        int count = 0;
        for (ControllerBindingSpec spec : current) {
            if (control.equals(spec.getControl())) count++;
        }
        return count;
    }

    public static synchronized float preview(ResourceLocation control, float raw) {
        ControllerBindingSpec match = null;
        for (ControllerBindingSpec spec : current) {
            if (control != null && control.equals(spec.getControl())) {
                match = spec;
                break;
            }
        }
        float value = AxisNormalizer.applyDeadzone(raw, ControllerConfig.deadzone());
        if (match == null) return value;
        value *= effectiveScale(match);
        if (effectiveInverted(match)) value = -value;
        return Math.max(-1.0F, Math.min(1.0F, value));
    }

    /** Resolves only the selected controller's bindings for a logical action. */
    public static synchronized InputValue resolve(InputAction action, ControllerSnapshot snapshot) {
        if (action == null || snapshot == null || !snapshot.isConnected()) return InputValue.NEUTRAL;
        InputValue result = InputValue.NEUTRAL;
        for (ControllerBindingSpec spec : current) {
            if (spec.getAction() != action || !spec.isBound()) continue;
            InputValue mapped = new InputBinding(spec.getControl(), action,
                    ControllerConfig.deadzone(), effectiveScale(spec), effectiveInverted(spec))
                    .map(snapshot.get(spec.getControl()));
            float axis = Math.abs(mapped.getAxis()) > Math.abs(result.getAxis())
                    ? mapped.getAxis() : result.getAxis();
            result = new InputValue(axis, result.isDown() || mapped.isDown(),
                    result.isPressed() || mapped.isPressed(),
                    result.isReleased() || mapped.isReleased());
        }
        return result;
    }

    public static float effectiveScale(ControllerBindingSpec spec) {
        switch (spec.getAction()) {
            case CAMERA_LOOK_X:
            case CAMERA_LOOK_Y:
                return spec.getScale() * ControllerConfig.lookSensitivity();
            case FLIGHT_PITCH:
            case FLIGHT_YAW:
            case FLIGHT_ROLL:
            case FLIGHT_RUDDER:
                return spec.getScale() * ControllerConfig.flightSensitivity();
            default:
                return spec.getScale();
        }
    }

    public static boolean effectiveInverted(ControllerBindingSpec spec) {
        boolean configured;
        switch (spec.getAction()) {
            case CAMERA_LOOK_X: configured = ControllerConfig.invertLookX(); break;
            case CAMERA_LOOK_Y: configured = ControllerConfig.invertLookY(); break;
            case FLIGHT_PITCH: configured = ControllerConfig.invertFlightPitch(); break;
            case FLIGHT_YAW: configured = ControllerConfig.invertFlightYaw(); break;
            case FLIGHT_ROLL: configured = ControllerConfig.invertFlightRoll(); break;
            default: configured = false;
        }
        return spec.isInverted() ^ configured;
    }

    public static synchronized List<String> serialize() {
        List<String> records = new ArrayList<>();
        for (ControllerBindingSpec spec : current) {
            records.add(spec.getAction().name() + "|" + spec.getSlot() + "|"
                    + (spec.getControl() == null ? "" : spec.getControl()) + "|"
                    + spec.getScale() + "|" + spec.isInverted());
        }
        return records;
    }

    public static synchronized List<String> defaultRecords() {
        List<ControllerBindingSpec> previous = current;
        current = copy(DEFAULTS);
        try { return serialize(); }
        finally { current = previous; }
    }

    public static synchronized void load(List<String> records) {
        Map<String, ControllerBindingSpec> parsed = new LinkedHashMap<>();
        if (records != null) {
            for (String record : records) {
                ControllerBindingSpec spec = parse(record);
                if (spec != null) parsed.put(spec.key(), spec);
            }
        }
        List<ControllerBindingSpec> result = new ArrayList<>();
        for (ControllerBindingSpec fallback : DEFAULTS) {
            result.add(parsed.getOrDefault(fallback.key(), fallback));
        }
        current = copy(result);
    }

    private static ControllerBindingSpec parse(String record) {
        if (record == null) return null;
        String[] parts = record.split("\\|", -1);
        if (parts.length != 5) return null;
        try {
            InputAction action = InputAction.valueOf(parts[0]);
            int slot = Integer.parseInt(parts[1]);
            ResourceLocation control = parts[2].isEmpty() ? null : new ResourceLocation(parts[2]);
            float scale = Float.parseFloat(parts[3]);
            boolean inverted = Boolean.parseBoolean(parts[4]);
            return new ControllerBindingSpec(action, slot, control, scale, inverted);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static List<ControllerBindingSpec> sanitize(List<ControllerBindingSpec> values) {
        Map<String, ControllerBindingSpec> supplied = new LinkedHashMap<>();
        if (values != null) for (ControllerBindingSpec value : values) {
            if (value != null) supplied.put(value.key(), value);
        }
        List<ControllerBindingSpec> result = new ArrayList<>();
        for (ControllerBindingSpec fallback : DEFAULTS) {
            result.add(supplied.getOrDefault(fallback.key(), fallback));
        }
        return result;
    }

    private static List<ControllerBindingSpec> copy(List<ControllerBindingSpec> source) {
        return new ArrayList<>(source);
    }

    private static List<ControllerBindingSpec> createDefaults() {
        EnumMap<InputAction, Integer> slots = new EnumMap<>(InputAction.class);
        List<ControllerBindingSpec> result = new ArrayList<>();
        add(result, slots, InputAction.CAMERA_LOOK_X, ControllerControls.RIGHT_STICK_X, false);
        add(result, slots, InputAction.CAMERA_LOOK_Y, ControllerControls.RIGHT_STICK_Y, false);
        add(result, slots, InputAction.CAMERA_ROLL, ControllerControls.RIGHT_SHOULDER, false);
        add(result, slots, InputAction.CAMERA_ROLL, ControllerControls.LEFT_SHOULDER, true);
        add(result, slots, InputAction.CAMERA_TRANSLATE_X, ControllerControls.LEFT_STICK_X, false);
        add(result, slots, InputAction.CAMERA_TRANSLATE_Y, ControllerControls.RIGHT_TRIGGER, false);
        add(result, slots, InputAction.CAMERA_TRANSLATE_Y, ControllerControls.LEFT_TRIGGER, true);
        add(result, slots, InputAction.CAMERA_TRANSLATE_Z, ControllerControls.LEFT_STICK_Y, true);
        add(result, slots, InputAction.PLAYER_MOVE_FORWARD, ControllerControls.LEFT_STICK_Y, true);
        add(result, slots, InputAction.PLAYER_MOVE_STRAFE, ControllerControls.LEFT_STICK_X, true);
        // MSFS-style gamepad layout: left stick is the flight stick, right stick remains camera.
        add(result, slots, InputAction.FLIGHT_PITCH, ControllerControls.LEFT_STICK_Y, false);
        add(result, slots, InputAction.FLIGHT_ROLL, ControllerControls.LEFT_STICK_X, false);
        add(result, slots, InputAction.FLIGHT_RUDDER, ControllerControls.TRIGGER_RUDDER, false);
        add(result, slots, InputAction.PLAYER_JUMP, ControllerControls.SOUTH, false);
        add(result, slots, InputAction.PLAYER_ATTACK, ControllerControls.WEST, false);
        add(result, slots, InputAction.PLAYER_USE, ControllerControls.EAST, false);
        add(result, slots, InputAction.PLAYER_INVENTORY, ControllerControls.START, false);
        add(result, slots, InputAction.GUI_CURSOR_X, ControllerControls.LEFT_STICK_X, false);
        add(result, slots, InputAction.GUI_CURSOR_Y, ControllerControls.LEFT_STICK_Y, false);
        add(result, slots, InputAction.GUI_SCROLL_Y, ControllerControls.RIGHT_STICK_Y, false);
        add(result, slots, InputAction.GUI_ACCEPT, ControllerControls.SOUTH, false);
        add(result, slots, InputAction.GUI_BACK, ControllerControls.EAST, false);
        add(result, slots, InputAction.GUI_SECONDARY, ControllerControls.WEST, false);
        add(result, slots, InputAction.GUI_QUICK_MOVE, ControllerControls.NORTH, false);
        add(result, slots, InputAction.GUI_NAV_UP, ControllerControls.DPAD_UP, false);
        add(result, slots, InputAction.GUI_NAV_DOWN, ControllerControls.DPAD_DOWN, false);
        add(result, slots, InputAction.GUI_NAV_LEFT, ControllerControls.DPAD_LEFT, false);
        add(result, slots, InputAction.GUI_NAV_RIGHT, ControllerControls.DPAD_RIGHT, false);
        add(result, slots, InputAction.GUI_PAGE_PREVIOUS, ControllerControls.LEFT_TRIGGER, false);
        add(result, slots, InputAction.GUI_PAGE_NEXT, ControllerControls.RIGHT_TRIGGER, false);
        add(result, slots, InputAction.CAMERA_TOGGLE_DRONE, ControllerControls.GUIDE, false);
        add(result, slots, InputAction.CAMERA_EXIT_DRONE, ControllerControls.BACK, false);
        add(result, slots, InputAction.CAMERA_TOGGLE_FREELOOK, ControllerControls.NORTH, false);
        add(result, slots, InputAction.CAMERA_TOGGLE_SHOULDER, ControllerControls.LEFT_STICK, false);
        add(result, slots, InputAction.CAMERA_TOGGLE_FREELOOK_CONTROL,
                ControllerControls.RIGHT_STICK, false);
        for (InputAction action : InputAction.values()) {
            if (!slots.containsKey(action)) add(result, slots, action, null, false);
        }
        result.sort((left, right) -> {
            int action = Integer.compare(left.getAction().ordinal(), right.getAction().ordinal());
            return action != 0 ? action : Integer.compare(left.getSlot(), right.getSlot());
        });
        return Collections.unmodifiableList(result);
    }

    private static void add(List<ControllerBindingSpec> result,
                            EnumMap<InputAction, Integer> slots, InputAction action,
                            ResourceLocation control, boolean inverted) {
        int slot = slots.getOrDefault(action, 0);
        slots.put(action, slot + 1);
        result.add(new ControllerBindingSpec(action, slot, control, 1.0F, inverted));
    }

}
