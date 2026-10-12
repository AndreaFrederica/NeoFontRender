package neofontrender.addons.input;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovementInput;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.input.InputDisposition;
import neofontrender.addons.api.input.InputFrame;
import neofontrender.addons.camera.CameraRuntime;

/**
 * Applies routed player actions to the final movement-input state of a tick.
 *
 * <p>1.12.2 exposed Forge's {@code InputUpdateEvent} for this; 1.7.10 has no such event, so
 * {@code MovementInputFromOptions.updatePlayerMoveState} invokes {@link #applyRoutedMovement}
 * after vanilla has recomputed the input and before the player applies it.</p>
 */
public final class PlayerMovementInputBridge {
    /** Retained for the module's event-bus registration, which is a no-op without handlers. */
    public static final PlayerMovementInputBridge INSTANCE = new PlayerMovementInputBridge();
    private static final float SNEAK_MULTIPLIER = 0.3F;

    private PlayerMovementInputBridge() {}

    /** Rewrites vanilla movement from the routed frame of the current input sample. */
    public static void applyRoutedMovement(MovementInput movement) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (movement == null || minecraft.currentScreen != null || !minecraft.inGameHasFocus) return;
        InputFrame frame = InputApi.getFrame(0.0F);
        if (frame.getSampleId() == 0L || !frame.getContext().isGameFocused()) return;

        EntityPlayer player = minecraft.thePlayer;
        boolean suppressStrafe = CameraRuntime.isFreeLookActive() && isFlightActive();
        State state = resolve(frame, movement.moveForward, movement.moveStrafe,
                movement.jump, movement.sneak, suppressStrafe);
        // Minecraft.timer is private in 1.7.10 with no accessor, and this runs inside the player
        // tick rather than the render seam; the completed tick is the closest available sample.
        Float cameraYaw = CameraRuntime.cursorLookMovementYaw(1.0F);
        float[] axes = cameraYaw == null || player == null
                ? new float[]{state.forward, state.strafe}
                : rotateAxes(state.forward, state.strafe,
                        cameraYaw - player.rotationYaw);
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
        // Keep jump on the vanilla tick path.  The frame is sampled at the render seam, so using
        // its physical jump state here can lag a press/release and interfere with double-jump
        // flight detection.  The fallback is MovementInputFromOptions' current tick result and
        // therefore also includes any KeyBinding extensions (for example controller input).
        boolean jump = frame == null || frame.getSampleId() == 0L
                || frame.disposition(InputAction.PLAYER_JUMP) != InputDisposition.BLOCK
                ? fallbackJump : false;
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
