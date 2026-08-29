package neofontrender.addons.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.input.InputBinding;
import neofontrender.addons.api.input.InputDeviceSample;
import neofontrender.addons.api.input.InputDisposition;
import neofontrender.addons.api.input.InputRegistration;
import neofontrender.addons.api.input.InputValue;
import neofontrender.addons.camera.CameraKeyBindings;

/**
 * Captures vanilla keyboard/mouse state at UIE's existing mouse sampling seam. It deliberately
 * only publishes raw intent; routing remains owned by {@link InputApi} contexts.
 */
public final class VanillaInputBridge {
    private static final double VIRTUAL_LOOK_UNITS_PER_SECOND = 1200.0D;
    private static final double DEFAULT_FRAME_SECONDS = 1.0D / 60.0D;
    private static final ResourceLocation ID = new ResourceLocation("neofontrender_ui_enhancements", "vanilla");
    private static final ResourceLocation LOOK_X = control("look_x");
    private static final ResourceLocation LOOK_Y = control("look_y");
    private static final ResourceLocation FORWARD = control("forward");
    private static final ResourceLocation STRAFE = control("strafe");
    private static final ResourceLocation VERTICAL = control("vertical");
    private static final ResourceLocation ROLL = control("camera_roll");
    private static final ResourceLocation JUMP = control("jump");
    private static final ResourceLocation SNEAK = control("sneak");
    private static final ResourceLocation SPRINT = control("sprint");
    private static final ResourceLocation ATTACK = control("attack");
    private static final ResourceLocation USE = control("use");
    private static final ResourceLocation PICK = control("pick");
    private static final ResourceLocation DROP = control("drop");
    private static final ResourceLocation INVENTORY = control("inventory");
    private static final ResourceLocation SWAP_HANDS = control("swap_hands");
    private static final ResourceLocation HOTBAR = control("hotbar");
    private static final ResourceLocation TOGGLE_DRONE = control("toggle_drone");
    private static final ResourceLocation EXIT_CAMERA = control("exit_camera");
    private static final ResourceLocation TOGGLE_FREE_LOOK = control("toggle_free_look");
    private static final ResourceLocation TOGGLE_SHOULDER = control("toggle_shoulder");
    private static final ResourceLocation SWAP_SHOULDER = control("swap_shoulder");
    private static final ResourceLocation FREELOOK_TOGGLE_CONTROL = control("freelook_toggle_control");
    private static final ResourceLocation TOGGLE_CURSOR_LOOK = control("toggle_cursor_look");

    private static volatile InputDeviceSample snapshot = InputDeviceSample.builder(ID).build();
    private static final java.util.Map<ResourceLocation, Boolean> previousButtons = new java.util.HashMap<>();
    private static boolean installed;
    @SuppressWarnings("unused") private static InputRegistration deviceRegistration;
    @SuppressWarnings("unused") private static InputRegistration bindingRegistration;

    private VanillaInputBridge() {}

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        deviceRegistration = InputApi.registerDeviceSource(ID, 0, frame -> snapshot);
        bindingRegistration = InputApi.registerBindingProvider(ID, 0, (frame, sink) -> {
            sink.bind(new InputBinding(LOOK_X, InputAction.CAMERA_LOOK_X));
            sink.bind(new InputBinding(LOOK_Y, InputAction.CAMERA_LOOK_Y));
            sink.bind(new InputBinding(ROLL, InputAction.CAMERA_ROLL));
            sink.bind(new InputBinding(FORWARD, InputAction.PLAYER_MOVE_FORWARD));
            sink.bind(new InputBinding(STRAFE, InputAction.PLAYER_MOVE_STRAFE));
            sink.bind(new InputBinding(FORWARD, InputAction.CAMERA_TRANSLATE_Z));
            sink.bind(new InputBinding(STRAFE, InputAction.CAMERA_TRANSLATE_X));
            sink.bind(new InputBinding(VERTICAL, InputAction.CAMERA_TRANSLATE_Y));
            sink.bind(new InputBinding(JUMP, InputAction.PLAYER_JUMP));
            sink.bind(new InputBinding(SNEAK, InputAction.PLAYER_SNEAK));
            sink.bind(new InputBinding(SPRINT, InputAction.PLAYER_SPRINT));
            sink.bind(new InputBinding(ATTACK, InputAction.PLAYER_ATTACK));
            sink.bind(new InputBinding(USE, InputAction.PLAYER_USE));
            sink.bind(new InputBinding(PICK, InputAction.PLAYER_PICK_BLOCK));
            sink.bind(new InputBinding(DROP, InputAction.PLAYER_DROP));
            sink.bind(new InputBinding(INVENTORY, InputAction.PLAYER_INVENTORY));
            sink.bind(new InputBinding(SWAP_HANDS, InputAction.PLAYER_SWAP_HANDS));
            sink.bind(new InputBinding(HOTBAR, InputAction.PLAYER_HOTBAR));
            sink.bind(new InputBinding(TOGGLE_DRONE, InputAction.CAMERA_TOGGLE_DRONE));
            sink.bind(new InputBinding(EXIT_CAMERA, InputAction.CAMERA_EXIT_DRONE));
            sink.bind(new InputBinding(TOGGLE_FREE_LOOK, InputAction.CAMERA_TOGGLE_FREELOOK));
            sink.bind(new InputBinding(TOGGLE_SHOULDER, InputAction.CAMERA_TOGGLE_SHOULDER));
            sink.bind(new InputBinding(SWAP_SHOULDER, InputAction.CAMERA_SWAP_SHOULDER));
            sink.bind(new InputBinding(FREELOOK_TOGGLE_CONTROL, InputAction.CAMERA_TOGGLE_FREELOOK_CONTROL));
            sink.bind(new InputBinding(TOGGLE_CURSOR_LOOK, InputAction.CAMERA_TOGGLE_CURSOR_LOOK));
        });
    }

    public static void capture(Minecraft minecraft, int mouseDeltaX, int mouseDeltaY) {
        if (minecraft == null || minecraft.gameSettings == null) return;
        if (!minecraft.inGameHasFocus) {
            reset();
            return;
        }
        GameSettings keys = minecraft.gameSettings;
        snapshot = InputDeviceSample.builder(ID)
                .put(LOOK_X, InputValue.axis(mouseAxis(mouseDeltaX)))
                .put(LOOK_Y, InputValue.axis(mouseAxis(mouseDeltaY)))
                .put(FORWARD, InputValue.axis(axis(keys.keyBindForward.isKeyDown(), keys.keyBindBack.isKeyDown())))
                .put(STRAFE, InputValue.axis(axis(keys.keyBindLeft.isKeyDown(), keys.keyBindRight.isKeyDown())))
                .put(VERTICAL, InputValue.axis(axis(keys.keyBindJump.isKeyDown(), keys.keyBindSneak.isKeyDown())))
                .put(ROLL, InputValue.axis(CameraKeyBindings.cameraRollAxis()))
                .put(JUMP, button(JUMP, keys.keyBindJump.isKeyDown()))
                .put(SNEAK, button(SNEAK, keys.keyBindSneak.isKeyDown()))
                .put(SPRINT, button(SPRINT, keys.keyBindSprint.isKeyDown()))
                .put(ATTACK, button(ATTACK, keys.keyBindAttack.isKeyDown()))
                .put(USE, button(USE, keys.keyBindUseItem.isKeyDown()))
                .put(PICK, button(PICK, keys.keyBindPickBlock.isKeyDown()))
                .put(DROP, button(DROP, keys.keyBindDrop.isKeyDown()))
                .put(INVENTORY, button(INVENTORY, keys.keyBindInventory.isKeyDown()))
                .put(SWAP_HANDS, button(SWAP_HANDS, keys.keyBindSwapHands.isKeyDown()))
                .put(HOTBAR, button(HOTBAR, anyHotbarKeyDown(keys)))
                .put(TOGGLE_DRONE, button(TOGGLE_DRONE, CameraKeyBindings.TOGGLE_DRONE.isKeyDown()))
                .put(EXIT_CAMERA, button(EXIT_CAMERA, CameraKeyBindings.EXIT_CAMERA.isKeyDown()))
                .put(TOGGLE_FREE_LOOK, button(TOGGLE_FREE_LOOK, CameraKeyBindings.TOGGLE_FREE_LOOK.isKeyDown()))
                .put(TOGGLE_SHOULDER, button(TOGGLE_SHOULDER, CameraKeyBindings.TOGGLE_SHOULDER.isKeyDown()))
                .put(SWAP_SHOULDER, button(SWAP_SHOULDER, CameraKeyBindings.SWAP_SHOULDER.isKeyDown()))
                .put(FREELOOK_TOGGLE_CONTROL, button(FREELOOK_TOGGLE_CONTROL, CameraKeyBindings.FREELOOK_TOGGLE_CONTROL.isKeyDown()))
                .put(TOGGLE_CURSOR_LOOK, button(TOGGLE_CURSOR_LOOK, CameraKeyBindings.TOGGLE_CURSOR_LOOK.isKeyDown()))
                .build();
    }

    /** Clears device history so focus regain produces fresh, deterministic button edges. */
    public static void reset() {
        snapshot = InputDeviceSample.builder(ID).build();
        previousButtons.clear();
    }

    /** Resolves one detached-camera axis without losing exact physical mouse deltas. */
    public static int resolveCameraDelta(int originalMouseDelta, int adjustedMouseDelta,
                                         boolean eventCanceled, InputValue routed,
                                         InputDisposition disposition, double frameSeconds) {
        if (eventCanceled || disposition == InputDisposition.BLOCK) return 0;
        if (originalMouseDelta != 0 || adjustedMouseDelta != 0) return adjustedMouseDelta;
        double seconds = Double.isFinite(frameSeconds) && frameSeconds > 0.0D
                ? Math.min(0.1D, frameSeconds) : DEFAULT_FRAME_SECONDS;
        return (int) Math.round((routed == null ? 0.0F : routed.getAxis())
                * VIRTUAL_LOOK_UNITS_PER_SECOND * seconds);
    }

    /** Drone keeps its legacy mouse sign; virtual controller look uses the opposite rig sign. */
    public static int resolveDroneCameraDeltaX(int originalMouseDelta, int adjustedMouseDelta,
                                               boolean eventCanceled, InputValue routed,
                                               InputDisposition disposition, double frameSeconds) {
        int delta = resolveCameraDelta(originalMouseDelta, adjustedMouseDelta, eventCanceled,
                routed, disposition, frameSeconds);
        if (originalMouseDelta == 0 && adjustedMouseDelta == 0 && routed != null
                && Math.abs(routed.getAxis()) > 1.0E-6F) return -delta;
        return delta;
    }

    private static ResourceLocation control(String path) { return new ResourceLocation(ID.getNamespace(), path); }
    private static InputValue button(ResourceLocation control, boolean down) {
        boolean previous = previousButtons.getOrDefault(control, false);
        previousButtons.put(control, down);
        return InputValue.button(down, down && !previous, !down && previous);
    }
    private static float axis(boolean positive, boolean negative) {
        return (positive ? 1.0F : 0.0F) - (negative ? 1.0F : 0.0F);
    }
    private static float mouseAxis(int delta) {
        return Math.max(-1.0F, Math.min(1.0F, delta / 100.0F));
    }

    private static boolean anyHotbarKeyDown(GameSettings settings) {
        for (net.minecraft.client.settings.KeyBinding key : settings.keyBindsHotbar) {
            if (key.isKeyDown()) return true;
        }
        return false;
    }
}
