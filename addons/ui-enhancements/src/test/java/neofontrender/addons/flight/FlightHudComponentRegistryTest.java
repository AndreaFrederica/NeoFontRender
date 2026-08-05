package neofontrender.addons.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightHudComponentRegistryTest {
    @Test
    void everyBuiltInSchemaTypeHasOneReusableComponent() {
        FlightHudComponentRegistry registry = FlightHudComponentRegistry.BUILT_INS;
        assertEquals(13, registry.size());
        for (String type : new String[] {
                "STATUS", "FLIGHT_REFERENCE", "AIRSPEED_TAPE", "ALTITUDE_TAPE",
                "VERTICAL_SPEED", "HEADING_RIBBON", "HEADING_ARC", "HEADING_DIAL",
                "GROUND_SPEED", "DATUM", "INPUT_STICK", "AOA_GAUGE", "ENERGY_GAUGE"
        }) assertTrue(registry.contains(type), type);
    }
}
