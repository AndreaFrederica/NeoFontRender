package neofontrender.addons.electricelytra;

import neofontrender.addons.electricelytra.compat.ElectricElytraCompat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import neofontrender.addons.electricelytra.compat.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightVector;

/** Runtime bridge between Minecraft's blocks/tick motion and the aerodynamic model. */
public final class ElectricFlightPhysics {
    private ElectricFlightPhysics() {}

    public static boolean shouldReplaceVanillaTravel(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || !ElectricElytraCompat.isElytraFlying(entity)
                || entity.isInWater()) return false;
        ItemStack chest = EntityEquipmentSlot.getChest(entity);
        return ItemElectricElytra.usesAerodynamicFlightModel(chest);
    }

    public static void integrate(EntityLivingBase entity) {
        ItemStack chest = EntityEquipmentSlot.getChest(entity);
        FlightAttitude attitude = ElectricBodyAxis.sampleAttitude(entity, 1.0F);
        FlightVector forward = attitude.forward();
        Vec3 bodyAxis = Vec3.createVectorHelper(forward.x, forward.y, forward.z);
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
