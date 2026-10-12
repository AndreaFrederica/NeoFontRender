package neofontrender.addons.controller.sdl;

import net.minecraft.util.ResourceLocation;

/** Stable physical-control IDs exposed by the SDL adapter. */
public final class ControllerControls {
    public static final String NAMESPACE = "neofontrender_ui_enhancements_controller";
    public static final ResourceLocation DISCONNECTED_DEVICE = id("sdl/disconnected");

    public static final ResourceLocation LEFT_STICK_X = id("axis/left_stick_x");
    public static final ResourceLocation LEFT_STICK_Y = id("axis/left_stick_y");
    public static final ResourceLocation RIGHT_STICK_X = id("axis/right_stick_x");
    public static final ResourceLocation RIGHT_STICK_Y = id("axis/right_stick_y");
    public static final ResourceLocation LEFT_TRIGGER = id("axis/left_trigger");
    public static final ResourceLocation RIGHT_TRIGGER = id("axis/right_trigger");
    /** Composite pedal axis: right trigger minus left trigger. */
    public static final ResourceLocation TRIGGER_RUDDER = id("axis/trigger_rudder");

    public static final ResourceLocation SOUTH = id("button/south");
    public static final ResourceLocation EAST = id("button/east");
    public static final ResourceLocation WEST = id("button/west");
    public static final ResourceLocation NORTH = id("button/north");
    public static final ResourceLocation BACK = id("button/back");
    public static final ResourceLocation GUIDE = id("button/guide");
    public static final ResourceLocation START = id("button/start");
    public static final ResourceLocation LEFT_STICK = id("button/left_stick");
    public static final ResourceLocation RIGHT_STICK = id("button/right_stick");
    public static final ResourceLocation LEFT_SHOULDER = id("button/left_shoulder");
    public static final ResourceLocation RIGHT_SHOULDER = id("button/right_shoulder");
    public static final ResourceLocation DPAD_UP = id("button/dpad_up");
    public static final ResourceLocation DPAD_DOWN = id("button/dpad_down");
    public static final ResourceLocation DPAD_LEFT = id("button/dpad_left");
    public static final ResourceLocation DPAD_RIGHT = id("button/dpad_right");
    public static final ResourceLocation MISC_1 = id("button/misc_1");
    public static final ResourceLocation MISC_2 = id("button/misc_2");
    public static final ResourceLocation MISC_3 = id("button/misc_3");
    public static final ResourceLocation MISC_4 = id("button/misc_4");
    public static final ResourceLocation MISC_5 = id("button/misc_5");
    public static final ResourceLocation MISC_6 = id("button/misc_6");
    public static final ResourceLocation LEFT_PADDLE_1 = id("button/left_paddle_1");
    public static final ResourceLocation LEFT_PADDLE_2 = id("button/left_paddle_2");
    public static final ResourceLocation RIGHT_PADDLE_1 = id("button/right_paddle_1");
    public static final ResourceLocation RIGHT_PADDLE_2 = id("button/right_paddle_2");
    public static final ResourceLocation TOUCHPAD = id("button/touchpad");

    private ControllerControls() {}

    static ResourceLocation device(int sdlId) {
        return id("sdl/" + Integer.toUnsignedString(sdlId));
    }

    static ResourceLocation joystickAxis(int index) {
        return id("joystick/axis/" + index);
    }

    static ResourceLocation joystickButton(int index) {
        return id("joystick/button/" + index);
    }

    static ResourceLocation joystickHat(int index, String direction) {
        return id("joystick/hat/" + index + "/" + direction);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(NAMESPACE, path);
    }
}
