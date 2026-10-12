package neofontrender.addons.controller.sdl;

import dev.isxander.sdl.Sdl;
import dev.isxander.sdl.SdlGamepadHandle;
import dev.isxander.sdl.SdlJoystickHandle;
import dev.isxander.sdl.SdlJoystickId;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.input.InputDeviceSample;
import neofontrender.addons.api.input.InputValue;
import neofontrender.addons.controller.ControllerConfig;

import java.util.Objects;

import static dev.isxander.sdl.SdlGamepad.*;
import static dev.isxander.sdl.SdlJoystick.*;

/** One open SDL gamepad or generic joystick with normalized, edge-aware controls. */
final class SdlControllerDevice implements AutoCloseable {
    private static final int[] GAMEPAD_AXES = {
            SDL_GAMEPAD_AXIS_LEFTX, SDL_GAMEPAD_AXIS_LEFTY,
            SDL_GAMEPAD_AXIS_RIGHTX, SDL_GAMEPAD_AXIS_RIGHTY,
            SDL_GAMEPAD_AXIS_LEFT_TRIGGER, SDL_GAMEPAD_AXIS_RIGHT_TRIGGER
    };
    private static final ResourceLocation[] GAMEPAD_AXIS_IDS = {
            ControllerControls.LEFT_STICK_X, ControllerControls.LEFT_STICK_Y,
            ControllerControls.RIGHT_STICK_X, ControllerControls.RIGHT_STICK_Y,
            ControllerControls.LEFT_TRIGGER, ControllerControls.RIGHT_TRIGGER
    };
    private static final int[] GAMEPAD_BUTTONS = {
            SDL_GAMEPAD_BUTTON_SOUTH, SDL_GAMEPAD_BUTTON_EAST,
            SDL_GAMEPAD_BUTTON_WEST, SDL_GAMEPAD_BUTTON_NORTH,
            SDL_GAMEPAD_BUTTON_BACK, SDL_GAMEPAD_BUTTON_GUIDE, SDL_GAMEPAD_BUTTON_START,
            SDL_GAMEPAD_BUTTON_LEFT_STICK, SDL_GAMEPAD_BUTTON_RIGHT_STICK,
            SDL_GAMEPAD_BUTTON_LEFT_SHOULDER, SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER,
            SDL_GAMEPAD_BUTTON_DPAD_UP, SDL_GAMEPAD_BUTTON_DPAD_DOWN,
            SDL_GAMEPAD_BUTTON_DPAD_LEFT, SDL_GAMEPAD_BUTTON_DPAD_RIGHT,
            SDL_GAMEPAD_BUTTON_MISC1, SDL_GAMEPAD_BUTTON_MISC2,
            SDL_GAMEPAD_BUTTON_MISC3, SDL_GAMEPAD_BUTTON_MISC4,
            SDL_GAMEPAD_BUTTON_MISC5, SDL_GAMEPAD_BUTTON_MISC6,
            SDL_GAMEPAD_BUTTON_LEFT_PADDLE1, SDL_GAMEPAD_BUTTON_LEFT_PADDLE2,
            SDL_GAMEPAD_BUTTON_RIGHT_PADDLE1, SDL_GAMEPAD_BUTTON_RIGHT_PADDLE2,
            SDL_GAMEPAD_BUTTON_TOUCHPAD
    };
    private static final ResourceLocation[] GAMEPAD_BUTTON_IDS = {
            ControllerControls.SOUTH, ControllerControls.EAST,
            ControllerControls.WEST, ControllerControls.NORTH,
            ControllerControls.BACK, ControllerControls.GUIDE, ControllerControls.START,
            ControllerControls.LEFT_STICK, ControllerControls.RIGHT_STICK,
            ControllerControls.LEFT_SHOULDER, ControllerControls.RIGHT_SHOULDER,
            ControllerControls.DPAD_UP, ControllerControls.DPAD_DOWN,
            ControllerControls.DPAD_LEFT, ControllerControls.DPAD_RIGHT,
            ControllerControls.MISC_1, ControllerControls.MISC_2,
            ControllerControls.MISC_3, ControllerControls.MISC_4,
            ControllerControls.MISC_5, ControllerControls.MISC_6,
            ControllerControls.LEFT_PADDLE_1, ControllerControls.LEFT_PADDLE_2,
            ControllerControls.RIGHT_PADDLE_1, ControllerControls.RIGHT_PADDLE_2,
            ControllerControls.TOUCHPAD
    };

    private final Sdl sdl;
    private final SdlJoystickId id;
    private final ResourceLocation deviceId;
    private final String persistentId;
    private final String name;
    private final SdlGamepadHandle gamepad;
    private final SdlJoystickHandle joystick;
    private final int joystickAxes;
    private final int joystickButtons;
    private final int joystickHats;
    private final ControlStateTracker buttonStates = new ControlStateTracker();
    private boolean closed;

    private SdlControllerDevice(Sdl sdl, SdlJoystickId id,
                                SdlGamepadHandle gamepad, SdlJoystickHandle joystick) {
        this.sdl = Objects.requireNonNull(sdl, "sdl");
        this.id = Objects.requireNonNull(id, "id");
        this.deviceId = ControllerControls.device(id.value());
        this.gamepad = gamepad;
        this.joystick = joystick;
        String detectedName = gamepad == null
                ? sdl.joystick().SDL_GetJoystickName(joystick)
                : sdl.gamepad().SDL_GetGamepadName(gamepad);
        this.name = detectedName == null || detectedName.trim().isEmpty()
                ? "SDL controller " + id.value() : detectedName;
        String serial = gamepad == null
                ? sdl.joystick().SDL_GetJoystickSerial(joystick)
                : sdl.gamepad().SDL_GetGamepadSerial(gamepad);
        String path = gamepad == null
                ? sdl.joystick().SDL_GetJoystickPath(joystick)
                : sdl.gamepad().SDL_GetGamepadPath(gamepad);
        this.persistentId = SdlDeviceIdentity.create(
                String.valueOf(sdl.joystick().SDL_GetJoystickGUIDForID(id)), serial, path, name);
        this.joystickAxes = joystick == null ? 0 : sdl.joystick().SDL_GetNumJoystickAxes(joystick);
        this.joystickButtons = joystick == null ? 0 : sdl.joystick().SDL_GetNumJoystickButtons(joystick);
        this.joystickHats = joystick == null ? 0 : sdl.joystick().SDL_GetNumJoystickHats(joystick);
    }

    static SdlControllerDevice open(Sdl sdl, SdlJoystickId id) {
        if (sdl.gamepad().SDL_IsGamepad(id)) {
            SdlGamepadHandle gamepad = sdl.gamepad().SDL_OpenGamepad(id);
            if (gamepad == null) throw new IllegalStateException("Failed to open SDL gamepad " + id.value());
            return new SdlControllerDevice(sdl, id, gamepad, null);
        }
        SdlJoystickHandle joystick = sdl.joystick().SDL_OpenJoystick(id);
        if (joystick == null) throw new IllegalStateException("Failed to open SDL joystick " + id.value());
        return new SdlControllerDevice(sdl, id, null, joystick);
    }

    int sdlId() {
        return id.value();
    }

    ResourceLocation deviceId() { return deviceId; }
    String persistentId() { return persistentId; }

    boolean isGamepad() { return gamepad != null; }

    String mapping() {
        return gamepad == null ? "<joystick>"
                : String.valueOf(sdl.gamepad().SDL_GetGamepadMapping(gamepad));
    }

    boolean isConnected() {
        return !closed && (gamepad != null
                ? sdl.gamepad().SDL_GamepadConnected(gamepad)
                : sdl.joystick().SDL_JoystickConnected(joystick));
    }

    ControllerSnapshot snapshot() {
        if (!isConnected()) return null;
        InputDeviceSample.Builder sample = InputDeviceSample.builder(deviceId);
        if (gamepad != null) sampleGamepad(sample);
        else sampleJoystick(sample);
        InputDeviceSample built = sample.build();
        return new ControllerSnapshot(deviceId, name, gamepad != null,
                built.controls(), System.nanoTime());
    }

    private void sampleGamepad(InputDeviceSample.Builder sample) {
        float leftTrigger = 0.0F;
        float rightTrigger = 0.0F;
        for (int index = 0; index < GAMEPAD_AXES.length; index++) {
            int axis = GAMEPAD_AXES[index];
            if (sdl.gamepad().SDL_GamepadHasAxis(gamepad, axis)) {
                float value = AxisNormalizer.normalize(
                        sdl.gamepad().SDL_GetGamepadAxis(gamepad, axis), 0.0F);
                sample.put(GAMEPAD_AXIS_IDS[index], InputValue.axis(value));
                if (GAMEPAD_AXIS_IDS[index].equals(ControllerControls.LEFT_TRIGGER)) {
                    leftTrigger = Math.max(0.0F, value);
                } else if (GAMEPAD_AXIS_IDS[index].equals(ControllerControls.RIGHT_TRIGGER)) {
                    rightTrigger = Math.max(0.0F, value);
                }
            }
        }
        sample.put(ControllerControls.TRIGGER_RUDDER,
                InputValue.axis(rightTrigger - leftTrigger));
        for (int index = 0; index < GAMEPAD_BUTTONS.length; index++) {
            int button = GAMEPAD_BUTTONS[index];
            if (sdl.gamepad().SDL_GamepadHasButton(gamepad, button)) {
                ResourceLocation control = GAMEPAD_BUTTON_IDS[index];
                sample.put(control, buttonStates.button(control,
                        sdl.gamepad().SDL_GetGamepadButton(gamepad, button)));
            }
        }
    }

    private void sampleJoystick(InputDeviceSample.Builder sample) {
        for (int index = 0; index < joystickAxes; index++) {
            sample.put(ControllerControls.joystickAxis(index), InputValue.axis(
                    AxisNormalizer.normalize(sdl.joystick().SDL_GetJoystickAxis(joystick, index), 0.0F)));
        }
        for (int index = 0; index < joystickButtons; index++) {
            ResourceLocation control = ControllerControls.joystickButton(index);
            sample.put(control, buttonStates.button(control,
                    sdl.joystick().SDL_GetJoystickButton(joystick, index)));
        }
        for (int index = 0; index < joystickHats; index++) {
            byte hat = sdl.joystick().SDL_GetJoystickHat(joystick, index);
            sampleHat(sample, index, "up", SDL_HAT_UP, hat);
            sampleHat(sample, index, "right", SDL_HAT_RIGHT, hat);
            sampleHat(sample, index, "down", SDL_HAT_DOWN, hat);
            sampleHat(sample, index, "left", SDL_HAT_LEFT, hat);
        }
    }

    private void sampleHat(InputDeviceSample.Builder sample, int index, String direction,
                           byte flag, byte state) {
        ResourceLocation control = ControllerControls.joystickHat(index, direction);
        sample.put(control, buttonStates.button(control, (state & flag) != 0));
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        buttonStates.clear();
        if (gamepad != null) sdl.gamepad().SDL_CloseGamepad(gamepad);
        if (joystick != null) sdl.joystick().SDL_CloseJoystick(joystick);
    }
}
