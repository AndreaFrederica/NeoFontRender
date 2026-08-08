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
    void fireworkAddsSpeedAboveVanillaTargetInsteadOfBraking() {
        ElectricFireworkBoost.Velocity boosted = ElectricFireworkBoost.apply(
                80.0D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D, 24.0D, 108.0D);
        assertTrue(boosted.x > 80.0D);
        assertTrue(boosted.x <= 108.0D);
    }
}
