package neofontrender.addons.controller;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.controller.sdl.ControllerControls;

import java.util.LinkedHashMap;
import java.util.Map;

/** Stable physical-control categories shared by diagnostics, capture, and display. */
final class ControllerControlCatalog {
    private static final Map<ResourceLocation, String> LABELS = new LinkedHashMap<>();

    static {
        label(ControllerControls.LEFT_STICK_X, "left_stick_x");
        label(ControllerControls.LEFT_STICK_Y, "left_stick_y");
        label(ControllerControls.RIGHT_STICK_X, "right_stick_x");
        label(ControllerControls.RIGHT_STICK_Y, "right_stick_y");
        label(ControllerControls.LEFT_TRIGGER, "left_trigger");
        label(ControllerControls.RIGHT_TRIGGER, "right_trigger");
        label(ControllerControls.TRIGGER_RUDDER, "trigger_rudder");
        label(ControllerControls.SOUTH, "south");
        label(ControllerControls.EAST, "east");
        label(ControllerControls.WEST, "west");
        label(ControllerControls.NORTH, "north");
        label(ControllerControls.BACK, "back");
        label(ControllerControls.GUIDE, "guide");
        label(ControllerControls.START, "start");
        label(ControllerControls.LEFT_STICK, "left_stick");
        label(ControllerControls.RIGHT_STICK, "right_stick");
        label(ControllerControls.LEFT_SHOULDER, "left_shoulder");
        label(ControllerControls.RIGHT_SHOULDER, "right_shoulder");
        label(ControllerControls.DPAD_UP, "dpad_up");
        label(ControllerControls.DPAD_DOWN, "dpad_down");
        label(ControllerControls.DPAD_LEFT, "dpad_left");
        label(ControllerControls.DPAD_RIGHT, "dpad_right");
        label(ControllerControls.TOUCHPAD, "touchpad");
    }

    private ControllerControlCatalog() {}

    static boolean isAxis(ResourceLocation control) {
        if (control == null) return false;
        String path = control.getPath();
        return path.startsWith("axis/") || path.startsWith("joystick/axis/");
    }

    static String labelKey(ResourceLocation control) { return LABELS.get(control); }

    private static void label(ResourceLocation control, String key) { LABELS.put(control, key); }
}
