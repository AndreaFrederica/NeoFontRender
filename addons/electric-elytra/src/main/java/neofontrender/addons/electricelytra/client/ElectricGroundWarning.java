package neofontrender.addons.electricelytra.client;

import neofontrender.addons.electricelytra.compat.ElectricElytraCompat;

import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import neofontrender.addons.electricelytra.ElectricElytraConfig;
import neofontrender.addons.electricelytra.ElectricFlightDebug;
import neofontrender.addons.electricelytra.ElectricWarningLogic;
import neofontrender.addons.electricelytra.ItemElectricElytra;

/** Client radar-altimeter and forward terrain predictor, sampled at most once per game tick. */
final class ElectricGroundWarning {
    static final ElectricGroundWarning INSTANCE = new ElectricGroundWarning();
    private static final double NO_RETURN = Double.POSITIVE_INFINITY;

    private int entityId = Integer.MIN_VALUE;
    private int lastTick = Integer.MIN_VALUE;
    private int airborneStartTick;
    private double maximumTakeoffAltitude;
    private Sample last = Sample.EMPTY;

    private ElectricGroundWarning() {}

    Sample sample(EntityPlayerSP player, ItemStack stack) {
        if (player.getEntityId() == entityId && player.ticksExisted == lastTick) return last;
        if (player.getEntityId() != entityId) reset(player);
        lastTick = player.ticksExisted;

        boolean airborne = ElectricElytraCompat.isElytraFlying(player) && !player.onGround;
        double radioAltitude = radioAltitude(player.worldObj, player.posX, player.posY,
                player.posZ, ElectricElytraConfig.warningRadioAltitudeMaximum);
        if (!airborne) {
            airborneStartTick = player.ticksExisted;
            maximumTakeoffAltitude = Double.isFinite(radioAltitude) ? radioAltitude : 0.0D;
            last = new Sample(ElectricWarningLogic.Warning.NONE, radioAltitude, NO_RETURN);
            return last;
        }

        if (Double.isFinite(radioAltitude)) {
            maximumTakeoffAltitude = Math.max(maximumTakeoffAltitude, radioAltitude);
        }
        double takeoffAgeSeconds = (player.ticksExisted - airborneStartTick) / 20.0D;
        double altitudeLoss = takeoffAgeSeconds <= ElectricElytraConfig.warningDontSinkWindowSeconds
                && Double.isFinite(radioAltitude)
                ? Math.max(0.0D, maximumTakeoffAltitude - radioAltitude) : 0.0D;

        double velocityX = player.motionX * 20.0D;
        double velocityY = player.motionY * 20.0D;
        double velocityZ = player.motionZ * 20.0D;
        double groundSpeed = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        double airspeed = Math.sqrt(groundSpeed * groundSpeed + velocityY * velocityY);
        ElectricFlightDebug.Sample debug = ElectricFlightDebug.get(player);
        boolean aerodynamic = ItemElectricElytra.usesAerodynamicFlightModel(stack);
        double angleOfAttack = !aerodynamic || debug == null ? 0.0D
                : Math.toDegrees(debug.angleOfAttackRadians);
        double terrainSeconds = predictTerrain(player.worldObj, player.posX, player.posY,
                player.posZ, velocityX, velocityY, velocityZ);
        ElectricWarningLogic.Warning warning = ElectricWarningLogic.evaluate(airborne,
                radioAltitude, velocityY, groundSpeed, airspeed, angleOfAttack,
                aerodynamic && ItemElectricElytra.isFlapCapable(stack),
                aerodynamic ? ItemElectricElytra.getFlapSetting(stack) : 0,
                altitudeLoss, terrainSeconds);
        last = new Sample(warning, radioAltitude, terrainSeconds);
        return last;
    }

    private void reset(EntityPlayerSP player) {
        entityId = player.getEntityId();
        lastTick = Integer.MIN_VALUE;
        airborneStartTick = player.ticksExisted;
        maximumTakeoffAltitude = 0.0D;
        last = Sample.EMPTY;
    }

    private static double predictTerrain(World world, double x, double y, double z,
                                         double velocityX, double velocityY,
                                         double velocityZ) {
        double maximum = ElectricElytraConfig.warningTerrainTimeSeconds;
        for (double seconds = 0.5D; seconds <= maximum + 1.0E-6D; seconds += 0.5D) {
            double predictedX = x + velocityX * seconds;
            double predictedY = y + velocityY * seconds;
            double predictedZ = z + velocityZ * seconds;
            if (predictedY <= 0.0D) return seconds;
            int predictedXInt = (int) Math.floor(predictedX);
            int predictedYInt = (int) Math.floor(predictedY);
            int predictedZInt = (int) Math.floor(predictedZ);
            if (!world.blockExists(predictedXInt, predictedYInt, predictedZInt)) continue;
            Block state = world.getBlock(predictedXInt, predictedYInt, predictedZInt);
            if (state.getMaterial().blocksMovement()) return seconds;
            double clearance = radioAltitude(world, predictedX, predictedY,
                    predictedZ, ElectricElytraConfig.warningRadioAltitudeMaximum);
            if (Double.isFinite(clearance) && clearance <= 1.5D) return seconds;
        }
        return NO_RETURN;
    }

    private static double radioAltitude(World world, double x, double y, double z,
                                        double maximumRange) {
        double endY = Math.max(0.0D, y - maximumRange);
        MovingObjectPosition hit = world.func_147447_a(Vec3.createVectorHelper(x, y + 0.05D, z),
                Vec3.createVectorHelper(x, endY, z), false, true, false);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                || hit.hitVec == null) {
            return NO_RETURN;
        }
        return Math.max(0.0D, y - hit.hitVec.yCoord);
    }

    static final class Sample {
        static final Sample EMPTY = new Sample(ElectricWarningLogic.Warning.NONE,
                NO_RETURN, NO_RETURN);
        final ElectricWarningLogic.Warning warning;
        final double radioAltitude;
        final double terrainSeconds;

        Sample(ElectricWarningLogic.Warning warning, double radioAltitude,
               double terrainSeconds) {
            this.warning = warning;
            this.radioAltitude = radioAltitude;
            this.terrainSeconds = terrainSeconds;
        }
    }
}
