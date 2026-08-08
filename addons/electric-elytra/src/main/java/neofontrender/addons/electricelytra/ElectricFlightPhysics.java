package neofontrender.addons.electricelytra;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightVector;

/** Runtime bridge between Minecraft's blocks/tick motion and the aerodynamic model. */
public final class ElectricFlightPhysics {
    private ElectricFlightPhysics() {}

    public static boolean shouldReplaceVanillaTravel(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || !entity.isElytraFlying()
                || entity.isInWater() || entity.isInLava()) return false;
        ItemStack chest = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        return ItemElectricElytra.usesAerodynamicFlightModel(chest);
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
