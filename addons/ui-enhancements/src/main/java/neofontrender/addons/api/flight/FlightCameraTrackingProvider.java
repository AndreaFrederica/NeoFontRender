package neofontrender.addons.api.flight;

import net.minecraft.client.entity.EntityPlayerSP;

/** Supplies a physical attitude for UIE's independent aircraft-camera tracking loop. */
@FunctionalInterface
public interface FlightCameraTrackingProvider {
    FlightCameraTracking tracking(EntityPlayerSP player, float partialTicks);
}
