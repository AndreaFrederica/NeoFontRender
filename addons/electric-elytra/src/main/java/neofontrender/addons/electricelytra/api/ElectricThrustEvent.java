package neofontrender.addons.electricelytra.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/** Mutable hook posted immediately before API-managed additive vanilla-Elytra thrust. */
@Cancelable
public final class ElectricThrustEvent extends Event {
    private final EntityLivingBase entity;
    private final ElectricAircraft aircraft;
    private Vec3d direction;
    private double accelerationBlocksPerSecondSquared;
    private double speedLimitBlocksPerSecond;

    public ElectricThrustEvent(EntityLivingBase entity, ElectricAircraft aircraft,
                               Vec3d direction, double acceleration, double speedLimit) {
        this.entity = entity;
        this.aircraft = aircraft;
        setDirection(direction);
        setAccelerationBlocksPerSecondSquared(acceleration);
        setSpeedLimitBlocksPerSecond(speedLimit);
    }

    public EntityLivingBase getEntity() { return entity; }
    public ElectricAircraft getAircraft() { return aircraft; }
    public Vec3d getDirection() { return direction; }
    public double getAccelerationBlocksPerSecondSquared() {
        return accelerationBlocksPerSecondSquared;
    }
    public double getSpeedLimitBlocksPerSecond() { return speedLimitBlocksPerSecond; }

    public void setDirection(Vec3d value) {
        if (value == null || value.lengthSquared() < 1.0E-12D) value = entity.getLookVec();
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
