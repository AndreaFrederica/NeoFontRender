package neofontrender.addons.electricelytra;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ElectricControlRegressionTest {
    @Test
    void creativeFlightNeverStartsElectricElytraFlight() {
        assertFalse(ElectricFlightController.shouldStartFlight(true, false,
                false, false, false, true, -0.1D));
        assertFalse(ElectricFlightController.shouldStartFlight(true, false,
                false, false, true, true, -0.1D));
        assertTrue(ElectricFlightController.shouldStartFlight(false, false,
                false, false, false, true, -0.1D));
    }

    @Test
    void runningEngineDoesNotForceFlightWithoutTakeoffInput() {
        assertFalse(ElectricFlightController.shouldStartFlight(false, true,
                false, false, true, true, 0.0D));
        assertFalse(ElectricFlightController.shouldStartFlight(false, false,
                false, false, true, false, -0.1D));
        assertTrue(ElectricFlightController.shouldStartFlight(false, false,
                false, false, true, true, 0.1D));
    }

    @Test
    void activeAerodynamicFlightSurvivesVanillaElytraValidation() {
        assertTrue(ElectricFlightController.shouldMaintainAerodynamicFlight(
                true, false, false, false, false, true));
        assertFalse(ElectricFlightController.shouldMaintainAerodynamicFlight(
                false, false, false, false, false, true));
        assertFalse(ElectricFlightController.shouldMaintainAerodynamicFlight(
                true, false, true, false, false, true));
        assertFalse(ElectricFlightController.shouldMaintainAerodynamicFlight(
                true, false, false, false, false, false));
    }

    @Test
    void clientFlightLatchSurvivesVanillaClearingTheFlightFlag() {
        assertTrue(ElectricFlightController.shouldMaintainClientAerodynamicFlight(
                false, false, true, false, true, false, false,
                true, true, 100, true, 0.0D));
        assertTrue(ElectricFlightController.shouldMaintainClientAerodynamicFlight(
                true, false, true, false, false, false, false,
                true, false, 0, false, 0.1D));
        assertFalse(ElectricFlightController.shouldMaintainClientAerodynamicFlight(
                false, false, true, false, false, false, false,
                true, false, 0, false, 0.1D));
        assertFalse(ElectricFlightController.shouldMaintainClientAerodynamicFlight(
                true, false, true, false, true, false, false,
                true, true, 100, false, 0.0D));
        assertFalse(ElectricFlightController.shouldMaintainClientAerodynamicFlight(
                true, true, true, false, false, false, false,
                false, true, 100, true, -0.1D));
        assertFalse(ElectricFlightController.shouldMaintainClientAerodynamicFlight(
                true, true, true, false, false, true, false,
                true, true, 100, true, -0.1D));
    }

    @Test
    void takeoffKeepsTheInitializedBodyAttitude() {
        assertFalse(ElectricFlightController.shouldResetGroundAttitude(true, true));
        assertTrue(ElectricFlightController.shouldResetGroundAttitude(true, false));
        assertFalse(ElectricFlightController.shouldResetGroundAttitude(false, false));
    }

    @Test
    void clientTakeoffPredictionUsesAerodynamicsBeforeFlagSynchronization() {
        assertTrue(ElectricFlightPhysics.shouldUseAerodynamicSolver(
                false, true, false, false, true));
        assertTrue(ElectricFlightPhysics.shouldUseAerodynamicSolver(
                true, false, false, false, true));
        assertFalse(ElectricFlightPhysics.shouldUseAerodynamicSolver(
                false, false, false, false, true));
        assertFalse(ElectricFlightPhysics.shouldUseAerodynamicSolver(
                false, true, true, false, true));
        assertFalse(ElectricFlightPhysics.shouldUseAerodynamicSolver(
                false, true, false, false, false));
    }

    @Test
    void fireworkAddsSpeedAboveVanillaTargetInsteadOfBraking() {
        ElectricFireworkBoost.Velocity boosted = ElectricFireworkBoost.apply(
                80.0D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D, 24.0D, 108.0D);
        assertTrue(boosted.x > 80.0D);
        assertTrue(boosted.x <= 108.0D);
    }
}
