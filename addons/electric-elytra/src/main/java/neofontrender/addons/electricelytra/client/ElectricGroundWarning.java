package neofontrender.addons.electricelytra.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
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

        boolean airborne = player.isElytraFlying() && !player.onGround;
        double radioAltitude = radioAltitude(player.world, player.posX, player.posY,
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
        double terrainSeconds = predictTerrain(player.world, player.posX, player.posY,
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
            BlockPos predicted = new BlockPos(predictedX, predictedY, predictedZ);
            if (!world.isBlockLoaded(predicted)) continue;
            IBlockState state = world.getBlockState(predicted);
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
        RayTraceResult hit = world.rayTraceBlocks(new Vec3d(x, y + 0.05D, z),
                new Vec3d(x, endY, z), false, true, false);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.hitVec == null) {
            return NO_RETURN;
        }
        return Math.max(0.0D, y - hit.hitVec.y);
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
