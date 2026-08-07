package neofontrender.addons.electricelytra;

/** Pure, game-scaled GPWS/TAWS warning envelope used by the client HUD. */
public final class ElectricWarningLogic {
    private ElectricWarningLogic() {}

    public static Warning evaluate(boolean airborne, double radioAltitude,
                                   double verticalSpeed, double groundSpeed,
                                   double airspeed, double angleOfAttackDegrees,
                                   boolean flapCapable, int flapSetting,
                                   double altitudeLossAfterTakeoff,
                                   double predictedTerrainSeconds) {
        if (!ElectricElytraConfig.warningEnabled || !airborne
                || !Double.isFinite(radioAltitude) || radioAltitude < 0.0D) {
            return Warning.NONE;
        }

        double sinkRate = Math.max(0.0D, -verticalSpeed);
        double impactSeconds = sinkRate > 1.0E-6D
                ? radioAltitude / sinkRate : Double.POSITIVE_INFINITY;
        boolean radarRange = radioAltitude <= ElectricElytraConfig.warningRadioAltitudeMaximum;

        // A terrain intersection inside the inner envelope has the highest priority.
        if (radarRange && sinkRate >= ElectricElytraConfig.warningMinimumSinkRate
                && impactSeconds <= ElectricElytraConfig.warningPullUpTimeSeconds) {
            return Warning.PULL_UP;
        }
        if (Double.isFinite(predictedTerrainSeconds)
                && predictedTerrainSeconds <= ElectricElytraConfig.warningPullUpTimeSeconds) {
            return Warning.PULL_UP;
        }

        double stallThreshold = Math.max(0.0D, ElectricElytraConfig.stallAngleDegrees
                - ElectricElytraConfig.warningStallMarginDegrees);
        if (airspeed >= ElectricElytraConfig.minimumAerodynamicSpeed * 2.0D
                && Math.abs(angleOfAttackDegrees) >= stallThreshold) {
            return Warning.STALL;
        }

        if (Double.isFinite(predictedTerrainSeconds)
                && predictedTerrainSeconds <= ElectricElytraConfig.warningTerrainTimeSeconds) {
            return Warning.TERRAIN;
        }
        if (radarRange
                && radioAltitude <= ElectricElytraConfig.warningSinkRateMaximumHeight
                && sinkRate >= ElectricElytraConfig.warningMinimumSinkRate
                + radioAltitude * ElectricElytraConfig.warningSinkRateHeightGain) {
            return Warning.SINK_RATE;
        }
        if (flapCapable && flapSetting == 0
                && radioAltitude <= ElectricElytraConfig.warningFlapHeight
                && sinkRate >= 0.5D
                && groundSpeed <= ElectricElytraConfig.warningFlapMaximumGroundSpeed) {
            return Warning.TOO_LOW_FLAPS;
        }
        if (altitudeLossAfterTakeoff >= ElectricElytraConfig.warningDontSinkAltitudeLoss
                && radioAltitude <= ElectricElytraConfig.warningSinkRateMaximumHeight) {
            return Warning.DONT_SINK;
        }
        return Warning.NONE;
    }

    public enum Warning {
        NONE("", false),
        DONT_SINK("DON'T SINK", false),
        TOO_LOW_FLAPS("TOO LOW FLAPS", false),
        SINK_RATE("SINK RATE", false),
        TERRAIN("TERRAIN", false),
        STALL("STALL", true),
        PULL_UP("PULL UP", true);

        public final String message;
        public final boolean urgent;

        Warning(String message, boolean urgent) {
            this.message = message;
            this.urgent = urgent;
        }
    }
}
