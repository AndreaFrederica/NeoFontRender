package neofontrender.addons.api.input;

import org.junit.jupiter.api.Test;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraMouseInputEventTest {
    @Test
    void handlersCanTransformOrConsumeIndividualRawAxes() {
        CameraMouseInputEvent event = new CameraMouseInputEvent(null, 0.5F, 12, -7);

        event.setDeltaX(4);
        event.consumeVertical();

        assertEquals(12, event.getOriginalDeltaX());
        assertEquals(-7, event.getOriginalDeltaY());
        assertEquals(4, event.getDeltaX());
        assertEquals(0, event.getDeltaY());
        assertEquals(0.5F, event.getPartialTicks());
    }

    @Test
    void eventIsMarkedForForgeToGenerateCancellationSupport() {
        // Forge adds the isCancelable override to event subclasses through its launch-time
        // transformer. Plain JUnit does not run that transformer, so inspect the contract here.
        assertTrue(CameraMouseInputEvent.class.isAnnotationPresent(Cancelable.class));
    }
}
