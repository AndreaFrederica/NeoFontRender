package neofontrender.addons.electricelytra;

import neofontrender.api.config.NfrConfigApi;
import neofontrender.api.config.NfrConfigFile;
import neofontrender.api.config.NfrConfigStorage;

public final class ElectricElytraConfig {
    public static int energyCapacity = 240_000;
    public static int idleEnergyPerTick = 8;
    public static int cruiseEnergyPerTick = 60;
    public static int liftEnergyPerTick = 110;
    public static double hardSpeedLimitBlocksPerSecond = 108.0D;
    public static double maximumThrustAcceleration = 12.0D;
    public static double fireworkBoostAcceleration = 24.0D;
    public static double takeoffVelocity = 0.38D;
    public static double gravityAcceleration = 9.81D;
    public static double airDensity = 1.225D;
    public static double totalMass = 90.0D;
    public static double wingArea = 2.2D;
    public static double zeroLiftDragCoefficient = 0.065D;
    public static double liftCoefficientAtZeroAngle = 0.25D;
    public static double liftCurveSlope = 4.5D;
    public static double maximumLiftCoefficient = 1.35D;
    public static double stallAngleDegrees = 17.0D;
    public static double wingAspectRatio = 4.0D;
    public static double oswaldEfficiency = 0.75D;
    public static double stallDragCoefficient = 0.90D;
    public static double minimumAerodynamicSpeed = 1.0D;
    public static double bodyAxisTurnRateDegreesPerSecond = 120.0D;
    public static double bodyRollRateDegreesPerSecond = 360.0D;
    public static double sideForceCoefficientPerRadian = 0.80D;
    public static double rudderSideForceCoefficient = 0.28D;
    public static double yawMomentCoefficientPerRadian = 0.12D;
    public static double rudderYawMomentCoefficient = 0.08D;
    public static double yawRateMomentCoefficient = 0.80D;
    public static double sideslipSaturationDegrees = 45.0D;
    public static double yawReferenceLength = 2.0D;
    public static double yawInertia = 80.0D;
    public static double yawRateDamping = 2.4D;
    public static double maximumYawAccelerationDegreesPerSecondSquared = 180.0D;
    public static double sasAttitudeResponsePerSecond = 3.5D;
    public static double sasYawRateDamping = 6.0D;
    public static double sasManualInputDeadzone = 0.05D;
    public static double takeoffFlapLiftIncrement = 0.25D;
    public static double takeoffFlapMaximumLiftIncrement = 0.25D;
    public static double takeoffFlapDragIncrement = 0.025D;
    public static double landingFlapLiftIncrement = 0.55D;
    public static double landingFlapMaximumLiftIncrement = 0.55D;
    public static double landingFlapDragIncrement = 0.10D;
    public static int throttleStepPercent = 1;
    public static double hudSpeedSmoothingSeconds = 0.16D;
    public static double hudPositionX = -1.0D;
    public static double hudPositionY = -1.0D;
    public static boolean warningEnabled = true;
    public static double warningRadioAltitudeMaximum = 256.0D;
    public static double warningPullUpTimeSeconds = 3.0D;
    public static double warningTerrainTimeSeconds = 6.0D;
    public static double warningMinimumSinkRate = 3.5D;
    public static double warningSinkRateHeightGain = 0.04D;
    public static double warningSinkRateMaximumHeight = 160.0D;
    public static double warningFlapHeight = 18.0D;
    public static double warningFlapMaximumGroundSpeed = 45.0D;
    public static double warningStallMarginDegrees = 2.0D;
    public static double warningDontSinkAltitudeLoss = 8.0D;
    public static double warningDontSinkWindowSeconds = 15.0D;
    private static NfrConfigFile file;

    private ElectricElytraConfig() {}

    public static synchronized void load() {
        if (file == null) {
            file = NfrConfigApi.builder(ElectricElytraMod.MOD_ID)
                    .storage(NfrConfigStorage.INDEPENDENT)
                    .fileName("neofontrender-electric-elytra.toml")
                    .open();
        }
        file.define("energy.capacity", energyCapacity,
                        "Internal Forge Energy capacity. Newly crafted Electric Elytra starts full.")
                .define("energy.idlePerTick", idleEnergyPerTick,
                        "FE used per tick while the engine is armed at idle.")
                .define("energy.cruisePerTick", cruiseEnergyPerTick,
                        "FE used per tick during sustained powered flight at full throttle.")
                .define("energy.liftPerTick", liftEnergyPerTick,
                        "FE used per tick during the standing launch impulse at full throttle.")
                .define("flight.hardSpeedLimitBlocksPerSecond", hardSpeedLimitBlocksPerSecond,
                        "Absolute velocity clamp in blocks per second.")
                .define("flight.maximumThrustAcceleration", maximumThrustAcceleration,
                        "Full-throttle acceleration in blocks per second squared, applied only along the feet-to-head body axis.")
                .define("flight.fireworkBoostAcceleration", fireworkBoostAcceleration,
                        "Additive firework acceleration for Electric Elytra; never pulls speed toward vanilla's target.")
                .define("flight.takeoffImpulseBlocksPerTick", takeoffVelocity,
                        "One-time upward launch impulse; sustained lift is aerodynamic.")
                .define("aerodynamics.gravityAcceleration", gravityAcceleration,
                        "Downward acceleration used by the replacement flight model.")
                .define("aerodynamics.airDensity", airDensity,
                        "Air density used by dynamic pressure q = 0.5 * rho * V^2.")
                .define("aerodynamics.totalMass", totalMass,
                        "Combined player and equipment mass in kilograms.")
                .define("aerodynamics.wingArea", wingArea,
                        "Electric Elytra reference wing area in square metres.")
                .define("aerodynamics.zeroLiftDragCoefficient", zeroLiftDragCoefficient,
                        "Parasite/body drag coefficient before induced and stall drag.")
                .define("aerodynamics.liftCoefficientAtZeroAngle", liftCoefficientAtZeroAngle,
                        "Cambered-wing lift coefficient at zero angle of attack.")
                .define("aerodynamics.liftCurveSlope", liftCurveSlope,
                        "Lift coefficient change per radian before stall.")
                .define("aerodynamics.maximumLiftCoefficient", maximumLiftCoefficient,
                        "Maximum absolute lift coefficient before stall decay.")
                .define("aerodynamics.stallAngleDegrees", stallAngleDegrees,
                        "Absolute angle of attack at which separated-flow penalties begin.")
                .define("aerodynamics.wingAspectRatio", wingAspectRatio,
                        "Wing aspect ratio used for induced drag.")
                .define("aerodynamics.oswaldEfficiency", oswaldEfficiency,
                        "Finite-wing efficiency factor used for induced drag.")
                .define("aerodynamics.stallDragCoefficient", stallDragCoefficient,
                        "Additional drag coefficient at fully separated flow.")
                .define("aerodynamics.minimumSpeed", minimumAerodynamicSpeed,
                        "Airspeed below which aerodynamic coefficients are not applied.")
                .define("flight.bodyAxisTurnRateDegreesPerSecond", bodyAxisTurnRateDegreesPerSecond,
                        "Maximum angular speed of the rendered feet-to-head body/thrust axis.")
                .define("flight.bodyRollRateDegreesPerSecond", bodyRollRateDegreesPerSecond,
                        "Maximum physical roll rate around the feet-to-head body axis.")
                .define("directional.sideForceCoefficientPerRadian", sideForceCoefficientPerRadian,
                        "Side-force coefficient slope opposing aerodynamic sideslip.")
                .define("directional.rudderSideForceCoefficient", rudderSideForceCoefficient,
                        "Side-force coefficient generated by full aerodynamic rudder input.")
                .define("directional.yawMomentCoefficientPerRadian", yawMomentCoefficientPerRadian,
                        "Weathercock yaw-moment coefficient slope caused by sideslip.")
                .define("directional.rudderYawMomentCoefficient", rudderYawMomentCoefficient,
                        "Yaw-moment coefficient generated by full aerodynamic rudder input.")
                .define("directional.yawRateMomentCoefficient", yawRateMomentCoefficient,
                        "Aerodynamic yaw-rate damping coefficient using r*b/(2V).")
                .define("directional.sideslipSaturationDegrees", sideslipSaturationDegrees,
                        "Soft saturation angle for large-sideslip yawing moments.")
                .define("directional.referenceLength", yawReferenceLength,
                        "Body reference length used by the aerodynamic yaw moment.")
                .define("directional.yawInertia", yawInertia,
                        "Effective yaw moment of inertia in kilogram square metres.")
                .define("directional.yawRateDamping", yawRateDamping,
                        "Passive yaw-rate damping per second.")
                .define("directional.maximumYawAccelerationDegreesPerSecondSquared",
                        maximumYawAccelerationDegreesPerSecondSquared,
                        "Safety clamp for aerodynamic yaw angular acceleration.")
                .define("sas.attitudeResponsePerSecond", sasAttitudeResponsePerSecond,
                        "Advanced Elytra SAS proportional attitude-hold response.")
                .define("sas.yawRateDamping", sasYawRateDamping,
                        "Advanced Elytra SAS yaw-rate damping per second.")
                .define("sas.manualInputDeadzone", sasManualInputDeadzone,
                        "Virtual-stick magnitude that overrides SAS hold and recentres its target.")
                .define("flaps.takeoff.liftCoefficientIncrement", takeoffFlapLiftIncrement,
                        "Zero-angle lift coefficient added by the TO flap detent.")
                .define("flaps.takeoff.maximumLiftIncrement", takeoffFlapMaximumLiftIncrement,
                        "Maximum lift coefficient added by the TO flap detent.")
                .define("flaps.takeoff.dragCoefficientIncrement", takeoffFlapDragIncrement,
                        "Parasite drag coefficient added by the TO flap detent.")
                .define("flaps.landing.liftCoefficientIncrement", landingFlapLiftIncrement,
                        "Zero-angle lift coefficient added by the LDG flap detent.")
                .define("flaps.landing.maximumLiftIncrement", landingFlapMaximumLiftIncrement,
                        "Maximum lift coefficient added by the LDG flap detent.")
                .define("flaps.landing.dragCoefficientIncrement", landingFlapDragIncrement,
                        "Parasite drag coefficient added by the LDG flap detent.")
                .define("controls.throttleStepPercent", throttleStepPercent,
                        "Percentage changed by each throttle increase/decrease key press.")
                .define("hud.speedSmoothingSeconds", hudSpeedSmoothingSeconds,
                        "Time constant used to damp small speed-tape corrections; zero disables it.")
                .define("hud.positionX", hudPositionX,
                        "Normalized draggable HUD X position. -1 uses the theme-aware default.")
                .define("hud.positionY", hudPositionY,
                        "Normalized draggable HUD Y position. -1 uses the theme-aware default.")
                .define("warnings.enabled", warningEnabled,
                        "Show terrain, sink-rate, stall, flap, and post-takeoff warnings on the flight HUD.")
                .define("warnings.radioAltitudeMaximum", warningRadioAltitudeMaximum,
                        "Maximum radar-altimeter range in blocks. No ground return above this distance.")
                .define("warnings.pullUpTimeSeconds", warningPullUpTimeSeconds,
                        "Predicted seconds to terrain at which a red PULL UP warning is issued.")
                .define("warnings.terrainTimeSeconds", warningTerrainTimeSeconds,
                        "Forward-looking seconds to terrain at which an amber TERRAIN caution is issued.")
                .define("warnings.minimumSinkRate", warningMinimumSinkRate,
                        "Minimum downward speed in blocks per second for a SINK RATE warning.")
                .define("warnings.sinkRateHeightGain", warningSinkRateHeightGain,
                        "Extra SINK RATE threshold per block of radio altitude, forming a sloped warning envelope.")
                .define("warnings.sinkRateMaximumHeight", warningSinkRateMaximumHeight,
                        "Maximum radio altitude in blocks at which SINK RATE can be issued.")
                .define("warnings.flapHeight", warningFlapHeight,
                        "Radio altitude below which a slow descent with flaps UP warns TOO LOW FLAPS.")
                .define("warnings.flapMaximumGroundSpeed", warningFlapMaximumGroundSpeed,
                        "Maximum ground speed for the TOO LOW FLAPS landing-configuration warning.")
                .define("warnings.stallMarginDegrees", warningStallMarginDegrees,
                        "STALL warning margin below the configured aerodynamic stall angle.")
                .define("warnings.dontSinkAltitudeLoss", warningDontSinkAltitudeLoss,
                        "Altitude loss after takeoff that triggers DON'T SINK.")
                .define("warnings.dontSinkWindowSeconds", warningDontSinkWindowSeconds,
                        "Time after takeoff during which the DON'T SINK mode remains armed.")
                .save();

        energyCapacity = file.getInt("energy.capacity", energyCapacity, 1_000, 20_000_000);
        idleEnergyPerTick = file.getInt("energy.idlePerTick", idleEnergyPerTick, 0, 100_000);
        cruiseEnergyPerTick = file.getInt("energy.cruisePerTick", cruiseEnergyPerTick, 1, 100_000);
        liftEnergyPerTick = file.getInt("energy.liftPerTick", liftEnergyPerTick, 1, 100_000);
        hardSpeedLimitBlocksPerSecond = file.getDouble("flight.hardSpeedLimitBlocksPerSecond",
                hardSpeedLimitBlocksPerSecond, 10.0D, 600.0D);
        maximumThrustAcceleration = file.getDouble("flight.maximumThrustAcceleration",
                maximumThrustAcceleration, 0.1D, 100.0D);
        fireworkBoostAcceleration = file.getDouble("flight.fireworkBoostAcceleration",
                fireworkBoostAcceleration, 0.1D, 200.0D);
        takeoffVelocity = file.getDouble("flight.takeoffImpulseBlocksPerTick",
                takeoffVelocity, 0.05D, 2.0D);
        gravityAcceleration = file.getDouble("aerodynamics.gravityAcceleration",
                gravityAcceleration, 0.1D, 100.0D);
        airDensity = file.getDouble("aerodynamics.airDensity", airDensity, 0.01D, 10.0D);
        totalMass = file.getDouble("aerodynamics.totalMass", totalMass, 10.0D, 1_000.0D);
        wingArea = file.getDouble("aerodynamics.wingArea", wingArea, 0.1D, 100.0D);
        zeroLiftDragCoefficient = file.getDouble("aerodynamics.zeroLiftDragCoefficient",
                zeroLiftDragCoefficient, 0.001D, 2.0D);
        liftCoefficientAtZeroAngle = file.getDouble("aerodynamics.liftCoefficientAtZeroAngle",
                liftCoefficientAtZeroAngle, -2.0D, 2.0D);
        liftCurveSlope = file.getDouble("aerodynamics.liftCurveSlope",
                liftCurveSlope, 0.1D, 10.0D);
        maximumLiftCoefficient = file.getDouble("aerodynamics.maximumLiftCoefficient",
                maximumLiftCoefficient, 0.1D, 5.0D);
        stallAngleDegrees = file.getDouble("aerodynamics.stallAngleDegrees",
                stallAngleDegrees, 3.0D, 45.0D);
        wingAspectRatio = file.getDouble("aerodynamics.wingAspectRatio",
                wingAspectRatio, 0.5D, 20.0D);
        oswaldEfficiency = file.getDouble("aerodynamics.oswaldEfficiency",
                oswaldEfficiency, 0.1D, 1.0D);
        stallDragCoefficient = file.getDouble("aerodynamics.stallDragCoefficient",
                stallDragCoefficient, 0.0D, 5.0D);
        minimumAerodynamicSpeed = file.getDouble("aerodynamics.minimumSpeed",
                minimumAerodynamicSpeed, 0.0D, 20.0D);
        bodyAxisTurnRateDegreesPerSecond = file.getDouble(
                "flight.bodyAxisTurnRateDegreesPerSecond",
                bodyAxisTurnRateDegreesPerSecond, 10.0D, 720.0D);
        bodyRollRateDegreesPerSecond = file.getDouble("flight.bodyRollRateDegreesPerSecond",
                bodyRollRateDegreesPerSecond, 10.0D, 1_440.0D);
        sideForceCoefficientPerRadian = file.getDouble(
                "directional.sideForceCoefficientPerRadian",
                sideForceCoefficientPerRadian, 0.0D, 5.0D);
        rudderSideForceCoefficient = file.getDouble("directional.rudderSideForceCoefficient",
                rudderSideForceCoefficient, 0.0D, 3.0D);
        yawMomentCoefficientPerRadian = file.getDouble(
                "directional.yawMomentCoefficientPerRadian",
                yawMomentCoefficientPerRadian, 0.0D, 2.0D);
        rudderYawMomentCoefficient = file.getDouble("directional.rudderYawMomentCoefficient",
                rudderYawMomentCoefficient, 0.0D, 2.0D);
        yawRateMomentCoefficient = file.getDouble("directional.yawRateMomentCoefficient",
                yawRateMomentCoefficient, 0.0D, 5.0D);
        sideslipSaturationDegrees = file.getDouble("directional.sideslipSaturationDegrees",
                sideslipSaturationDegrees, 5.0D, 90.0D);
        yawReferenceLength = file.getDouble("directional.referenceLength",
                yawReferenceLength, 0.1D, 20.0D);
        yawInertia = file.getDouble("directional.yawInertia", yawInertia, 1.0D, 10_000.0D);
        yawRateDamping = file.getDouble("directional.yawRateDamping",
                yawRateDamping, 0.0D, 20.0D);
        maximumYawAccelerationDegreesPerSecondSquared = file.getDouble(
                "directional.maximumYawAccelerationDegreesPerSecondSquared",
                maximumYawAccelerationDegreesPerSecondSquared, 1.0D, 2_000.0D);
        sasAttitudeResponsePerSecond = file.getDouble("sas.attitudeResponsePerSecond",
                sasAttitudeResponsePerSecond, 0.1D, 20.0D);
        sasYawRateDamping = file.getDouble("sas.yawRateDamping",
                sasYawRateDamping, 0.0D, 30.0D);
        sasManualInputDeadzone = file.getDouble("sas.manualInputDeadzone",
                sasManualInputDeadzone, 0.0D, 0.5D);
        takeoffFlapLiftIncrement = file.getDouble("flaps.takeoff.liftCoefficientIncrement",
                takeoffFlapLiftIncrement, 0.0D, 3.0D);
        takeoffFlapMaximumLiftIncrement = file.getDouble("flaps.takeoff.maximumLiftIncrement",
                takeoffFlapMaximumLiftIncrement, 0.0D, 3.0D);
        takeoffFlapDragIncrement = file.getDouble("flaps.takeoff.dragCoefficientIncrement",
                takeoffFlapDragIncrement, 0.0D, 2.0D);
        landingFlapLiftIncrement = file.getDouble("flaps.landing.liftCoefficientIncrement",
                landingFlapLiftIncrement, 0.0D, 3.0D);
        landingFlapMaximumLiftIncrement = file.getDouble("flaps.landing.maximumLiftIncrement",
                landingFlapMaximumLiftIncrement, 0.0D, 3.0D);
        landingFlapDragIncrement = file.getDouble("flaps.landing.dragCoefficientIncrement",
                landingFlapDragIncrement, 0.0D, 2.0D);
        throttleStepPercent = file.getInt("controls.throttleStepPercent",
                throttleStepPercent, 1, 25);
        hudSpeedSmoothingSeconds = file.getDouble("hud.speedSmoothingSeconds",
                hudSpeedSmoothingSeconds, 0.0D, 1.0D);
        hudPositionX = file.getDouble("hud.positionX", hudPositionX, -1.0D, 1.0D);
        hudPositionY = file.getDouble("hud.positionY", hudPositionY, -1.0D, 1.0D);
        warningEnabled = file.getBoolean("warnings.enabled", warningEnabled);
        warningRadioAltitudeMaximum = file.getDouble("warnings.radioAltitudeMaximum",
                warningRadioAltitudeMaximum, 8.0D, 1_024.0D);
        warningPullUpTimeSeconds = file.getDouble("warnings.pullUpTimeSeconds",
                warningPullUpTimeSeconds, 0.5D, 10.0D);
        warningTerrainTimeSeconds = file.getDouble("warnings.terrainTimeSeconds",
                warningTerrainTimeSeconds, warningPullUpTimeSeconds, 20.0D);
        warningMinimumSinkRate = file.getDouble("warnings.minimumSinkRate",
                warningMinimumSinkRate, 0.5D, 50.0D);
        warningSinkRateHeightGain = file.getDouble("warnings.sinkRateHeightGain",
                warningSinkRateHeightGain, 0.0D, 1.0D);
        warningSinkRateMaximumHeight = file.getDouble("warnings.sinkRateMaximumHeight",
                warningSinkRateMaximumHeight, 5.0D, 1_024.0D);
        warningFlapHeight = file.getDouble("warnings.flapHeight",
                warningFlapHeight, 1.0D, 128.0D);
        warningFlapMaximumGroundSpeed = file.getDouble("warnings.flapMaximumGroundSpeed",
                warningFlapMaximumGroundSpeed, 1.0D, 200.0D);
        warningStallMarginDegrees = file.getDouble("warnings.stallMarginDegrees",
                warningStallMarginDegrees, 0.0D, 15.0D);
        warningDontSinkAltitudeLoss = file.getDouble("warnings.dontSinkAltitudeLoss",
                warningDontSinkAltitudeLoss, 1.0D, 128.0D);
        warningDontSinkWindowSeconds = file.getDouble("warnings.dontSinkWindowSeconds",
                warningDontSinkWindowSeconds, 1.0D, 120.0D);
    }

    public static synchronized void saveHudPosition(double x, double y) {
        hudPositionX = Math.max(0.0D, Math.min(1.0D, x));
        hudPositionY = Math.max(0.0D, Math.min(1.0D, y));
        if (file != null) {
            file.set("hud.positionX", hudPositionX)
                    .set("hud.positionY", hudPositionY)
                    .save();
        }
    }
}
