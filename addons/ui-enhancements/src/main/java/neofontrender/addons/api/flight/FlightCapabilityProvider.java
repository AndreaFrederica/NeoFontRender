package neofontrender.addons.api.flight;

import net.minecraft.client.entity.EntityPlayerSP;

/** Decides whether one flight capability is available for the local player. */
@FunctionalInterface
public interface FlightCapabilityProvider {
    FlightDecision decide(EntityPlayerSP player, FlightCapability capability,
                          boolean builtInDefault);
}
