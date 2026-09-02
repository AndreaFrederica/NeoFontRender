package neofontrender.addons.electricelytra;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightVector;

import java.util.Map;
import java.util.WeakHashMap;

/** Runtime bridge between Minecraft's blocks/tick motion and the aerodynamic model. */
public final class ElectricFlightPhysics {
    private static final Map<EntityLivingBase, Boolean> CLIENT_PREDICTIONS = new WeakHashMap<>();

    private ElectricFlightPhysics() {}

    public static boolean shouldReplaceVanillaTravel(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return false;
        ItemStack chest = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        return shouldUseAerodynamicSolver(entity.isElytraFlying(),
                entity.world.isRemote && hasClientPrediction(entity), entity.isInWater(),
                entity.isInLava(), ItemElectricElytra.usesAerodynamicFlightModel(chest));
    }

    public static synchronized void setClientPrediction(EntityLivingBase entity,
                                                         boolean active) {
        if (entity == null || !entity.world.isRemote) return;
        if (active) CLIENT_PREDICTIONS.put(entity, Boolean.TRUE);
        else CLIENT_PREDICTIONS.remove(entity);
    }

    public static synchronized boolean hasClientPrediction(EntityLivingBase entity) {
        return CLIENT_PREDICTIONS.containsKey(entity);
    }

    static boolean shouldUseAerodynamicSolver(boolean elytraFlying, boolean clientPrediction,
                                              boolean inWater, boolean inLava,
                                              boolean aerodynamicModel) {
        return (elytraFlying || clientPrediction) && !inWater && !inLava && aerodynamicModel;
    }

    public static void integrate(EntityLivingBase entity) {
        ItemStack chest = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        FlightAttitude attitude = ElectricBodyAxis.sampleAttitude(entity, 1.0F);
        FlightVector forward = attitude.forward();
        Vec3d bodyAxis = new Vec3d(forward.x, forward.y, forward.z);
        double velocityX = entity.motionX * 20.0D;
        double velocityY = entity.motionY * 20.0D;
        double velocityZ = entity.motionZ * 20.0D;
        boolean engineEnabled = ItemElectricElytra.isEngineEnabled(chest);
        double throttle = ItemElectricElytra.getThrottle(chest) / 100.0D;
        double rudderCommand = ElectricBodyAxis.sampleRudderCommand(entity);
        double fireworkAcceleration = ElectricFireworkBoostState.isActive(entity)
                ? ElectricElytraConfig.fireworkBoostAcceleration : 0.0D;
        ElectricFlightMath.FlightStep step = ElectricFlightMath.step(
                velocityX, velocityY, velocityZ, attitude, engineEnabled, throttle,
                ItemElectricElytra.getFlapSetting(chest), rudderCommand,
                fireworkAcceleration);
        ElectricFlightDebug.update(entity, bodyAxis, velocityX, velocityY, velocityZ,
                step, (engineEnabled
                        ? ElectricElytraConfig.maximumThrustAcceleration * throttle : 0.0D)
                        + fireworkAcceleration);
        entity.motionX = step.velocityX / 20.0D;
        entity.motionY = step.velocityY / 20.0D;
        entity.motionZ = step.velocityZ / 20.0D;
    }
}
