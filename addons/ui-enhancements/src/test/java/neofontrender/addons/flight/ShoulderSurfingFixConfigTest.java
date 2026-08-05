package neofontrender.addons.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShoulderSurfingFixConfigTest {
    @Test
    void exposesFiveModesAndFallsBackToPatched() {
        String original = ShoulderSurfingFixConfig.mode();
        try {
            ShoulderSurfingFixConfig.setMode(ShoulderSurfingFixConfig.MODE_ADAPTIVE);
            assertTrue(ShoulderSurfingFixConfig.adaptive());
            ShoulderSurfingFixConfig.setMode(ShoulderSurfingFixConfig.MODE_DUAL);
            assertTrue(ShoulderSurfingFixConfig.dual());
            ShoulderSurfingFixConfig.setMode(ShoulderSurfingFixConfig.MODE_STATIC);
            assertTrue(ShoulderSurfingFixConfig.staticMode());
            ShoulderSurfingFixConfig.setMode(ShoulderSurfingFixConfig.MODE_OFF);
            assertFalse(ShoulderSurfingFixConfig.enabled());
            ShoulderSurfingFixConfig.setMode("unknown");
            assertTrue(ShoulderSurfingFixConfig.patched());
            assertEquals(ShoulderSurfingFixConfig.MODE_PATCHED,
                    ShoulderSurfingFixConfig.mode());
        } finally {
            ShoulderSurfingFixConfig.setMode(original);
        }
    }
}
