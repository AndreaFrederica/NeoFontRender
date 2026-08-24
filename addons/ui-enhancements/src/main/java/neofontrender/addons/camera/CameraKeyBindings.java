package neofontrender.addons.camera;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

/** Optional compatibility bindings; the normal entry point is the vanilla F5 cycle. */
public final class CameraKeyBindings {
    public static final KeyBinding TOGGLE_DRONE = binding("camera_drone", Keyboard.KEY_NONE);
    public static final KeyBinding TOGGLE_FREE_LOOK = binding("camera_free_look", Keyboard.KEY_NONE);
    public static final KeyBinding TOGGLE_SHOULDER = binding("camera_shoulder", Keyboard.KEY_NONE);
    public static final KeyBinding SWAP_SHOULDER = binding("camera_swap_shoulder", Keyboard.KEY_O);
    public static final KeyBinding EXIT_CAMERA = binding("camera_exit", Keyboard.KEY_NONE);
    public static final KeyBinding FREELOOK_TOGGLE_CONTROL = binding("camera_freelook_toggle_control", Keyboard.KEY_TAB);
    public static final KeyBinding FREELOOK_MOVE_UP = binding("camera_freelook_move_up", Keyboard.KEY_NUMPAD8);
    public static final KeyBinding FREELOOK_MOVE_DOWN = binding("camera_freelook_move_down", Keyboard.KEY_NUMPAD2);
    public static final KeyBinding FREELOOK_MOVE_LEFT = binding("camera_freelook_move_left", Keyboard.KEY_NUMPAD4);
    public static final KeyBinding FREELOOK_MOVE_RIGHT = binding("camera_freelook_move_right", Keyboard.KEY_NUMPAD6);
    public static final KeyBinding FREELOOK_MOVE_FORWARD = binding("camera_freelook_move_forward", Keyboard.KEY_NUMPAD9);
    public static final KeyBinding FREELOOK_MOVE_BACKWARD = binding("camera_freelook_move_backward", Keyboard.KEY_NUMPAD3);
    public static final KeyBinding FREELOOK_MOVE_RESET = binding("camera_freelook_move_reset", Keyboard.KEY_NUMPAD5);
    public static final KeyBinding DRONE_ROTATE_LEFT = binding("camera_drone_rotate_left", Keyboard.KEY_NUMPAD0);
    public static final KeyBinding DRONE_ROTATE_RIGHT = binding("camera_drone_rotate_right", Keyboard.KEY_NUMPADENTER);
    static final KeyBinding ADJUST_LEFT = binding("camera_adjust_left", Keyboard.KEY_LEFT);
    static final KeyBinding ADJUST_RIGHT = binding("camera_adjust_right", Keyboard.KEY_RIGHT);
    static final KeyBinding ADJUST_UP = binding("camera_adjust_up", Keyboard.KEY_PRIOR);
    static final KeyBinding ADJUST_DOWN = binding("camera_adjust_down", Keyboard.KEY_NEXT);
    static final KeyBinding ADJUST_IN = binding("camera_adjust_in", Keyboard.KEY_UP);
    static final KeyBinding ADJUST_OUT = binding("camera_adjust_out", Keyboard.KEY_DOWN);
    static final KeyBinding ROLL_LEFT = binding("camera_roll_left", Keyboard.KEY_Z);
    static final KeyBinding ROLL_RIGHT = binding("camera_roll_right", Keyboard.KEY_X);

    private CameraKeyBindings() {}

    private static KeyBinding binding(String path, int key) {
        return new KeyBinding("key.neofontrender_ui_enhancements." + path,
                key, "key.categories.neofontrender_ui_enhancements");
    }

    /** Dedicated camera roll control; physical mouse X remains an observation axis only. */
    public static float cameraRollAxis() {
        return (ROLL_LEFT.isKeyDown() ? 1.0F : 0.0F) - (ROLL_RIGHT.isKeyDown() ? 1.0F : 0.0F);
    }
}
