package neofontrender.addons.api.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlightControllerInputEventTest {
    @Test
    void controllerAxesAreFiniteAndNormalized() {
        FlightControllerInputEvent event = new FlightControllerInputEvent(null, 0.5F, 1.0D / 60.0D);
        event.setPitch(2.0F);
        event.setYaw(-3.0F);
        event.setRoll(Float.NaN);

        assertEquals(1.0F, event.getPitch());
        assertEquals(-1.0F, event.getYaw());
        assertEquals(0.0F, event.getRoll());
    }
}
