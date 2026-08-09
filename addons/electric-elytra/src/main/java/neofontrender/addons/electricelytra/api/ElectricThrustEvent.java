package neofontrender.addons.electricelytra.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;

/** Mutable hook posted immediately before API-managed additive vanilla-Elytra thrust. */
@Cancelable
public final class ElectricThrustEvent extends Event {
    private final EntityLivingBase entity;
    private final ElectricAircraft aircraft;
    private Vec3 direction;
    private double accelerationBlocksPerSecondSquared;
    private double speedLimitBlocksPerSecond;

    public ElectricThrustEvent(EntityLivingBase entity, ElectricAircraft aircraft,
                               Vec3 direction, double acceleration, double speedLimit) {
        this.entity = entity;
        this.aircraft = aircraft;
        setDirection(direction);
        setAccelerationBlocksPerSecondSquared(acceleration);
        setSpeedLimitBlocksPerSecond(speedLimit);
    }

    public EntityLivingBase getEntity() { return entity; }
    public ElectricAircraft getAircraft() { return aircraft; }
    public Vec3 getDirection() { return direction; }
    public double getAccelerationBlocksPerSecondSquared() {
        return accelerationBlocksPerSecondSquared;
    }
    public double getSpeedLimitBlocksPerSecond() { return speedLimitBlocksPerSecond; }

    public void setDirection(Vec3 value) {
        if (value == null || value.xCoord * value.xCoord + value.yCoord * value.yCoord
                + value.zCoord * value.zCoord < 1.0E-12D) value = entity.getLookVec();
        direction = value.normalize();
    }

    public void setAccelerationBlocksPerSecondSquared(double value) {
        accelerationBlocksPerSecondSquared = Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    public void setSpeedLimitBlocksPerSecond(double value) {
        speedLimitBlocksPerSecond = Double.isFinite(value) ? Math.max(0.0D, value)
                : Double.POSITIVE_INFINITY;
    }
}
