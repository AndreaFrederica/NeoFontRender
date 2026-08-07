package neofontrender.addons.api.flight;

import net.minecraft.client.entity.EntityPlayerSP;

/** Supplies an aircraft/body attitude that takes over UIE's conformal HUD orientation. */
@FunctionalInterface
public interface FlightHudAttitudeProvider {
    /** Return {@code null} when this provider is inactive so the next provider or camera fallback may run. */
    FlightHudAttitude attitude(EntityPlayerSP player, float partialTicks);
}
