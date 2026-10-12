package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.camera.CameraRuntime;
import neofontrender.addons.input.PlayerMovementInputBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clears vanilla player movement while the modal drone input context owns the camera,
 *  and blocks strafe during free-look + flight so A/D only controls flight yaw. */
@Mixin(MovementInputFromOptions.class)
public abstract class MixinMovementInputFromOptionsDroneGate {
    @Inject(method = "updatePlayerMoveState", at = @At("RETURN"), require = 1)
    private void nfrUi$blockDronePlayerMovement(CallbackInfo ci) {
        AccessorMovementInputState state = (AccessorMovementInputState) (Object) this;
        if (CameraRuntime.isDroneActive()) {
            if (InputApi.isBlocked(InputAction.PLAYER_MOVE_FORWARD)) state.nfrUi$setMoveForward(0.0F);
            if (InputApi.isBlocked(InputAction.PLAYER_MOVE_STRAFE)) state.nfrUi$setMoveStrafe(0.0F);
            if (InputApi.isBlocked(InputAction.PLAYER_JUMP)) state.nfrUi$setJump(false);
            if (InputApi.isBlocked(InputAction.PLAYER_SNEAK)) state.nfrUi$setSneak(false);
        } else if (CameraRuntime.isFreeLookActive() && isFlightActive()) {
            // Free-look + flight active: block strafe so A/D only controls flight yaw
            state.nfrUi$setMoveStrafe(0.0F);
        }
        // 1.12.2 applied the routed player actions from Forge's InputUpdateEvent; 1.7.10 has no such
        // event, so the routed frame lands at the end of vanilla's own input recomputation.
        PlayerMovementInputBridge.applyRoutedMovement((MovementInput) (Object) this);
    }

    private static boolean isFlightActive() {
        return FlightApi.isActive();
    }
}
