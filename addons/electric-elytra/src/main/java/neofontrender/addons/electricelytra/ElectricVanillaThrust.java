package neofontrender.addons.electricelytra;

import neofontrender.addons.electricelytra.compat.ElectricElytraCompat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.electricelytra.api.ElectricAircraft;
import neofontrender.addons.electricelytra.api.ElectricAircraftApi;
import neofontrender.addons.electricelytra.api.ElectricFlightModel;
import neofontrender.addons.electricelytra.api.ElectricThrustEvent;

/** Adds only electric thrust to Minecraft's untouched vanilla Elytra travel solver. */
public final class ElectricVanillaThrust {
    private static final double TICKS_PER_SECOND = 20.0D;

    private ElectricVanillaThrust() {}

    public static boolean shouldApply(EntityLivingBase entity) {
        if (entity == null || !ElectricElytraCompat.isElytraFlying(entity)) return false;
        ElectricAircraft aircraft = ElectricAircraftApi.query(entity);
        return aircraft != null && aircraft.getFlightModel() == ElectricFlightModel.VANILLA_ELYTRA
                && aircraft.isEngineEnabled() && aircraft.hasEnergy()
                && aircraft.getThrottleFraction() > 0.0D;
    }

    public static void apply(EntityLivingBase entity) {
        ElectricAircraft aircraft = ElectricAircraftApi.query(entity);
        if (aircraft == null || aircraft.getFlightModel() != ElectricFlightModel.VANILLA_ELYTRA)
            return;
        double throttle = Math.max(0.0D, Math.min(1.0D, aircraft.getThrottleFraction()));
        ElectricThrustEvent event = new ElectricThrustEvent(entity, aircraft,
                aircraft.getThrustDirection(entity),
                aircraft.getMaximumThrustAcceleration(entity) * throttle,
                aircraft.getSpeedLimitBlocksPerSecond(entity));
        if (MinecraftForge.EVENT_BUS.post(event)) return;
        Vec3 direction = event.getDirection();
        double deltaMotion = motionIncrement(
                event.getAccelerationBlocksPerSecondSquared(), 1.0D);
        entity.motionX += direction.xCoord * deltaMotion;
        entity.motionY += direction.yCoord * deltaMotion;
        entity.motionZ += direction.zCoord * deltaMotion;
        clampMotion(entity, event.getSpeedLimitBlocksPerSecond());
    }

    static double motionIncrement(double accelerationBlocksPerSecondSquared, double throttle) {
        return Math.max(0.0D, accelerationBlocksPerSecondSquared)
                * Math.max(0.0D, Math.min(1.0D, throttle))
                / (TICKS_PER_SECOND * TICKS_PER_SECOND);
    }

    private static void clampMotion(EntityLivingBase entity, double speedLimit) {
        double maximumMotion = speedLimit / TICKS_PER_SECOND;
        double speed = Math.sqrt(entity.motionX * entity.motionX + entity.motionY * entity.motionY
                + entity.motionZ * entity.motionZ);
        if (speed <= maximumMotion || speed <= 1.0E-9D) return;
        double scale = maximumMotion / speed;
        entity.motionX *= scale;
        entity.motionY *= scale;
        entity.motionZ *= scale;
    }
}
