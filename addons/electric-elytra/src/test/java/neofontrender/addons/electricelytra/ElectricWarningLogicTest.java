package neofontrender.addons.electricelytra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ElectricWarningLogicTest {
    @BeforeEach
    void defaults() {
        ElectricElytraConfig.warningEnabled = true;
        ElectricElytraConfig.warningRadioAltitudeMaximum = 256.0D;
        ElectricElytraConfig.warningPullUpTimeSeconds = 3.0D;
        ElectricElytraConfig.warningTerrainTimeSeconds = 6.0D;
        ElectricElytraConfig.warningMinimumSinkRate = 3.5D;
        ElectricElytraConfig.warningSinkRateHeightGain = 0.04D;
        ElectricElytraConfig.warningSinkRateMaximumHeight = 160.0D;
        ElectricElytraConfig.warningFlapHeight = 18.0D;
        ElectricElytraConfig.warningFlapMaximumGroundSpeed = 45.0D;
        ElectricElytraConfig.warningStallMarginDegrees = 2.0D;
        ElectricElytraConfig.warningDontSinkAltitudeLoss = 8.0D;
        ElectricElytraConfig.stallAngleDegrees = 17.0D;
        ElectricElytraConfig.minimumAerodynamicSpeed = 1.0D;
    }

    @Test
    void sinkRateEscalatesToPullUpInsideInnerEnvelope() {
        assertEquals(ElectricWarningLogic.Warning.SINK_RATE,
                warning(100.0D, -8.0D, 30.0D, 0.0D, false, 0, 0.0D,
                        Double.POSITIVE_INFINITY));
        assertEquals(ElectricWarningLogic.Warning.PULL_UP,
                warning(10.0D, -4.0D, 30.0D, 0.0D, false, 0, 0.0D,
                        Double.POSITIVE_INFINITY));
    }

    @Test
    void forwardTerrainEscalatesByPredictedImpactTime() {
        assertEquals(ElectricWarningLogic.Warning.TERRAIN,
                warning(120.0D, 0.0D, 60.0D, 0.0D, false, 0, 0.0D, 5.0D));
        assertEquals(ElectricWarningLogic.Warning.PULL_UP,
                warning(120.0D, 0.0D, 60.0D, 0.0D, false, 0, 0.0D, 2.5D));
    }

    @Test
    void warnsForStallAndLandingConfiguration() {
        assertEquals(ElectricWarningLogic.Warning.STALL,
                warning(80.0D, 0.0D, 20.0D, 15.0D, true, 0, 0.0D,
                        Double.POSITIVE_INFINITY));
        assertEquals(ElectricWarningLogic.Warning.TOO_LOW_FLAPS,
                warning(12.0D, -1.0D, 20.0D, 0.0D, true, 0, 0.0D,
                        Double.POSITIVE_INFINITY));
    }

    @Test
    void warnsForAltitudeLossOnlyInCallerArmedTakeoffWindow() {
        assertEquals(ElectricWarningLogic.Warning.DONT_SINK,
                warning(50.0D, -1.0D, 20.0D, 0.0D, false, 0, 9.0D,
                        Double.POSITIVE_INFINITY));
        assertEquals(ElectricWarningLogic.Warning.NONE,
                ElectricWarningLogic.evaluate(false, 5.0D, -20.0D, 20.0D,
                        28.0D, 0.0D, true, 0, 20.0D, 1.0D));
    }

    private static ElectricWarningLogic.Warning warning(double radioAltitude,
                                                        double verticalSpeed,
                                                        double groundSpeed,
                                                        double angleOfAttack,
                                                        boolean flapCapable,
                                                        int flapSetting,
                                                        double altitudeLoss,
                                                        double terrainSeconds) {
        double airspeed = Math.sqrt(groundSpeed * groundSpeed
                + verticalSpeed * verticalSpeed);
        return ElectricWarningLogic.evaluate(true, radioAltitude, verticalSpeed,
                groundSpeed, airspeed, angleOfAttack, flapCapable, flapSetting,
                altitudeLoss, terrainSeconds);
    }
}
