package neofontrender.addons.input;

import neofontrender.addons.api.input.InputDisposition;
import neofontrender.addons.api.input.InputValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VanillaInputBridgeTest {
    @Test
    void adjustedPhysicalMouseDeltaRemainsExact() {
        assertEquals(240, VanillaInputBridge.resolveCameraDelta(180, 240, false,
                InputValue.axis(1.0F), InputDisposition.CLAIM));
    }

    @Test
    void eventConsumptionAndInputBlockBothStopCameraMotion() {
        assertEquals(0, VanillaInputBridge.resolveCameraDelta(12, 0, false,
                InputValue.axis(0.12F), InputDisposition.CLAIM));
        assertEquals(0, VanillaInputBridge.resolveCameraDelta(12, 12, true,
                InputValue.axis(0.12F), InputDisposition.CLAIM));
        assertEquals(0, VanillaInputBridge.resolveCameraDelta(12, 12, false,
                InputValue.axis(0.12F), InputDisposition.BLOCK));
    }

    @Test
    void virtualAxisFillsAFrameWithoutPhysicalMouseMotion() {
        assertEquals(-65, VanillaInputBridge.resolveCameraDelta(0, 0, false,
                InputValue.axis(-0.65F), InputDisposition.CLAIM));
    }
}
