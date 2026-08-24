package neofontrender.addons.input;

import neofontrender.addons.api.input.InputDisposition;
import neofontrender.addons.api.input.InputValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VanillaInputBridgeTest {
    @Test
    void adjustedPhysicalMouseDeltaRemainsExact() {
        assertEquals(240, VanillaInputBridge.resolveCameraDelta(180, 240, false,
                InputValue.axis(1.0F), InputDisposition.CLAIM, 1.0D / 60.0D));
    }

    @Test
    void eventConsumptionAndInputBlockBothStopCameraMotion() {
        assertEquals(0, VanillaInputBridge.resolveCameraDelta(12, 0, false,
                InputValue.axis(0.12F), InputDisposition.CLAIM, 1.0D / 60.0D));
        assertEquals(0, VanillaInputBridge.resolveCameraDelta(12, 12, true,
                InputValue.axis(0.12F), InputDisposition.CLAIM, 1.0D / 60.0D));
        assertEquals(0, VanillaInputBridge.resolveCameraDelta(12, 12, false,
                InputValue.axis(0.12F), InputDisposition.BLOCK, 1.0D / 60.0D));
    }

    @Test
    void virtualAxisFillsAFrameWithoutPhysicalMouseMotion() {
        assertEquals(-13, VanillaInputBridge.resolveCameraDelta(0, 0, false,
                InputValue.axis(-0.65F), InputDisposition.CLAIM, 1.0D / 60.0D));
    }

    @Test
    void virtualLookSpeedDoesNotDependOnRenderRate() {
        int sixtyFps = VanillaInputBridge.resolveCameraDelta(0, 0, false,
                InputValue.axis(1.0F), InputDisposition.PASS, 1.0D / 60.0D);
        int twentyFps = VanillaInputBridge.resolveCameraDelta(0, 0, false,
                InputValue.axis(1.0F), InputDisposition.PASS, 1.0D / 20.0D);
        assertEquals(20, sixtyFps);
        assertEquals(60, twentyFps);
    }
}
