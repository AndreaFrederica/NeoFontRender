package neofontrender.addons.input;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovementInput;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputDisposition;
import neofontrender.addons.api.input.InputFrame;
import neofontrender.addons.camera.CameraRuntime;

/** Applies routed player actions to Forge 1.12's final movement-input event. */
public final class PlayerMovementInputBridge {
    public static final PlayerMovementInputBridge INSTANCE = new PlayerMovementInputBridge();
    private static final float SNEAK_MULTIPLIER = 0.3F;

    private PlayerMovementInputBridge() {}

    @SubscribeEvent
    public void onInputUpdate(InputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event == null || minecraft.currentScreen != null || !minecraft.inGameHasFocus) return;
        InputFrame frame = neofontrender.addons.api.input.InputApi.getFrame(0.0F);
        if (frame.getSampleId() == 0L || !frame.getContext().isGameFocused()) return;

        MovementInput movement = event.getMovementInput();
        boolean suppressStrafe = CameraRuntime.isFreeLookActive() && isFlightActive();
        State state = resolve(frame, movement.moveForward, movement.moveStrafe,
                movement.jump, movement.sneak, suppressStrafe);
        Float cameraYaw = CameraRuntime.cursorLookMovementYaw(minecraft.getRenderPartialTicks());
        float[] axes = cameraYaw == null || minecraft.player == null
                ? new float[]{state.forward, state.strafe}
                : rotateAxes(state.forward, state.strafe,
                        cameraYaw - minecraft.player.rotationYaw);
        movement.moveForward = axes[0];
        movement.moveStrafe = axes[1];
        movement.jump = state.jump;
        movement.sneak = state.sneak;
    }

    static float[] rotateAxes(float forward, float strafe, float yawDeltaDegrees) {
        if (!Float.isFinite(forward) || !Float.isFinite(strafe)
                || !Float.isFinite(yawDeltaDegrees)) return new float[]{0.0F, 0.0F};
        double radians = Math.toRadians(yawDeltaDegrees);
        double sine = Math.sin(radians);
        double cosine = Math.cos(radians);
        return new float[]{
                (float) (forward * cosine + strafe * sine),
                (float) (strafe * cosine - forward * sine)
        };
    }

    static State resolve(InputFrame frame, float fallbackForward, float fallbackStrafe,
                         boolean fallbackJump, boolean fallbackSneak,
                         boolean suppressStrafe) {
        float forward = axis(frame, InputAction.PLAYER_MOVE_FORWARD, fallbackForward);
        float strafe = suppressStrafe ? 0.0F
                : axis(frame, InputAction.PLAYER_MOVE_STRAFE, fallbackStrafe);
        boolean jump = button(frame, InputAction.PLAYER_JUMP, fallbackJump);
        boolean sneak = button(frame, InputAction.PLAYER_SNEAK, fallbackSneak);
        if (sneak) {
            forward *= SNEAK_MULTIPLIER;
            strafe *= SNEAK_MULTIPLIER;
        }
        return new State(forward, strafe, jump, sneak);
    }

    private static float axis(InputFrame frame, InputAction action, float fallback) {
        if (frame == null || frame.getSampleId() == 0L) return clamp(fallback);
        if (frame.disposition(action) == InputDisposition.BLOCK) return 0.0F;
        return clamp(frame.get(action).getAxis());
    }

    private static boolean button(InputFrame frame, InputAction action, boolean fallback) {
        if (frame == null || frame.getSampleId() == 0L) return fallback;
        if (frame.disposition(action) == InputDisposition.BLOCK) return false;
        return fallback || frame.get(action).isDown();
    }

    private static boolean isFlightActive() {
        return FlightApi.isActive();
    }

    private static float clamp(float value) {
        if (!Float.isFinite(value)) return 0.0F;
        return Math.max(-1.0F, Math.min(1.0F, value));
    }

    static final class State {
        final float forward;
        final float strafe;
        final boolean jump;
        final boolean sneak;

        State(float forward, float strafe, boolean jump, boolean sneak) {
            this.forward = forward;
            this.strafe = strafe;
            this.jump = jump;
            this.sneak = sneak;
        }
    }
}
