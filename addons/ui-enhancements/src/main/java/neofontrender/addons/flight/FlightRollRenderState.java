package neofontrender.addons.flight;

import net.minecraft.client.entity.AbstractClientPlayer;

/** Stable bridge used by the player-render mixin without exposing controller internals. */
public final class FlightRollRenderState {
    private FlightRollRenderState() {}

    public static float roll(AbstractClientPlayer player, float partialTicks) {
        return player == null ? 0.0F
                : FlightRollController.playerRollForEntity(player.getEntityId(), partialTicks);
    }
}
