package neofontrender.addons.api.flight;

import net.minecraft.client.entity.AbstractClientPlayer;

/** Supplies a rendered body axis, or {@code null} when the provider is inactive. */
@FunctionalInterface
public interface FlightBodyPoseProvider {
    FlightBodyPose pose(AbstractClientPlayer player, float partialTicks);
}
