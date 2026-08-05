package neofontrender.addons.api.flight.server;

import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightServerApiTest {
    @Test
    void providerCanAuthorizeNonElytraVehicleAndChangeRange() {
        FlightServerApi.configureDefaults(true, true, 180.0F);
        FlightServerRegistration registration = FlightServerApi.registerPolicyProvider(
                new ResourceLocation("test", "vehicle"), 100,
                (player, policy) -> policy.withElytraRequired(false)
                        .withMaximumRollSpeed(240.0F).withSynchronizationRange(384.0D));
        try {
            FlightServerPolicy policy = FlightServerApi.policyFor(null);
            assertTrue(policy.isEnabled());
            assertFalse(policy.isElytraRequired());
            assertEquals(240.0F, policy.getMaximumRollSpeed());
            assertEquals(384.0D, policy.getSynchronizationRange());
        } finally {
            registration.close();
        }
    }
}
