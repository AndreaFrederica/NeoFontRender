package neofontrender.addons.electricelytra.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;

/**
 * Ephemeral powered-aircraft view returned by a provider. Implementations should read current
 * synchronized state and must not retain the queried entity after the tick.
 */
public interface ElectricAircraft {
    ElectricFlightModel getFlightModel();
    boolean isEngineEnabled();
    boolean hasEnergy();

    /** Normalized throttle in [0, 1]. */
    double getThrottleFraction();

    /** Thrust acceleration in blocks/second squared before throttle is applied. */
    double getMaximumThrustAcceleration(EntityLivingBase entity);

    /** Absolute velocity cap in blocks/second, or positive infinity for no API-side cap. */
    double getSpeedLimitBlocksPerSecond(EntityLivingBase entity);

    /** Defaults to the entity view axis; providers may return a body or nozzle axis instead. */
    default Vec3 getThrustDirection(EntityLivingBase entity) {
        return entity.getLookVec();
    }
}
