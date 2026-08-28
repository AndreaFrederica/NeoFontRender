package neofontrender.addons.input;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.input.InputContext;
import neofontrender.addons.api.input.InputFlushReason;
import neofontrender.addons.api.input.InputRegistration;

/**
 * Modal input guard for free-look mode. In view-control mode, claims camera look axes so the
 * mouse rotates the detached camera. In player-control mode, does not claim the look axes,
 * allowing vanilla's player.turn() to rotate the player body. Camera roll remains detached.
 */
public final class FreeLookInputGuard implements AutoCloseable {
    private final InputRegistration registration;
    private boolean closed;

    private FreeLookInputGuard(InputRegistration registration) { this.registration = registration; }

    public static FreeLookInputGuard enter(boolean controlPlayer) {
        return enter(controlPlayer, new ResourceLocation(
                "neofontrender_ui_enhancements", "free_look"));
    }

    public static FreeLookInputGuard enter(boolean controlPlayer, ResourceLocation contextId) {
        InputContext.Builder builder = InputContext.builder(
                contextId, 5000)
                .claim(InputAction.CAMERA_ROLL);
        if (!controlPlayer) {
            builder.claim(InputAction.CAMERA_LOOK_X, InputAction.CAMERA_LOOK_Y);
        }
        InputRegistration registration = InputApi.pushContext(builder.build());
        InputApi.flush(InputFlushReason.MODE_ENTER);
        return new FreeLookInputGuard(registration);
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        registration.close();
        InputApi.flush(InputFlushReason.MODE_EXIT);
    }
}
