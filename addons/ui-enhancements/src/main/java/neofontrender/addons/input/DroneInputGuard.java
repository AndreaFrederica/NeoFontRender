package neofontrender.addons.input;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.input.InputContext;
import neofontrender.addons.api.input.InputFlushReason;
import neofontrender.addons.api.input.InputRegistration;
import neofontrender.addons.camera.CameraRuntime;

/**
 * Modal input guard used by the drone camera. The camera session owns movement/look actions while
 * player and Flight control actions are explicitly neutralized until this handle is closed.
 */
public final class DroneInputGuard implements AutoCloseable {
    private static final ResourceLocation ID = new ResourceLocation("neofontrender_ui_enhancements", "drone");
    private final InputRegistration registration;
    private boolean closed;

    private DroneInputGuard(InputRegistration registration) {
        this.registration = registration;
    }

    /** Installs the guard and emits the first neutral frame before camera control starts. */
    public static DroneInputGuard enter() {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getMinecraft();
        if (minecraft.player != null) minecraft.player.setSprinting(false);
        InputContext.Builder builder = InputContext.builder(ID, 10000)
                .claim(InputAction.CAMERA_LOOK_X, InputAction.CAMERA_LOOK_Y, InputAction.CAMERA_ROLL,
                        InputAction.CAMERA_ZOOM, InputAction.CAMERA_TRANSLATE_X,
                        InputAction.CAMERA_TRANSLATE_Y, InputAction.CAMERA_TRANSLATE_Z)
                .block(InputAction.FLIGHT_PITCH, InputAction.FLIGHT_YAW, InputAction.FLIGHT_ROLL,
                        InputAction.FLIGHT_RUDDER, InputAction.PLAYER_MOVE_FORWARD,
                        InputAction.PLAYER_MOVE_STRAFE, InputAction.PLAYER_JUMP,
                        InputAction.PLAYER_SNEAK, InputAction.PLAYER_SPRINT,
                        InputAction.PLAYER_PICK_BLOCK, InputAction.PLAYER_DROP,
                        InputAction.PLAYER_INVENTORY, InputAction.PLAYER_SWAP_HANDS,
                        InputAction.PLAYER_HOTBAR);
        if (!CameraRuntime.isDroneInteractionSettingEnabled()) {
            builder.block(InputAction.PLAYER_ATTACK, InputAction.PLAYER_USE);
        }
        InputRegistration registration = InputApi.pushContext(builder.build());
        InputApi.flush(InputFlushReason.MODE_ENTER);
        return new DroneInputGuard(registration);
    }

    public boolean isClosed() { return closed; }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        registration.close();
        InputApi.flush(InputFlushReason.MODE_EXIT);
    }
}
