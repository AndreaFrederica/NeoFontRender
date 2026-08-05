package neofontrender.addons.api.flight.server;

import net.minecraft.entity.player.EntityPlayerMP;

/** Transforms the effective policy for a player; providers run in descending priority order. */
@FunctionalInterface
public interface FlightServerPolicyProvider {
    FlightServerPolicy apply(EntityPlayerMP player, FlightServerPolicy current);
}
