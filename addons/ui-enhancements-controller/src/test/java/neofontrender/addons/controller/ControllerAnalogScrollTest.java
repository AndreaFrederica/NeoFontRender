package neofontrender.addons.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControllerAnalogScrollTest {
    @Test void emitsImmediatelyThenAccumulatesContinuously() {
        ControllerAnalogScroll scroll = new ControllerAnalogScroll();
        assertEquals(1, scroll.update(1.0F, 0.0D));
        assertEquals(0, scroll.update(1.0F, 0.02D));
        assertEquals(0, scroll.update(1.0F, 0.02D));
        assertEquals(1, scroll.update(1.0F, 0.01D));
    }

    @Test void integratedDistanceDoesNotDependOnFrameRate() {
        ControllerAnalogScroll atTwentyFps = new ControllerAnalogScroll();
        ControllerAnalogScroll atHundredFps = new ControllerAnalogScroll();
        int slow = atTwentyFps.update(0.5F, 0.0D);
        int fast = atHundredFps.update(0.5F, 0.0D);
        for (int frame = 0; frame < 20; frame++) slow += atTwentyFps.update(0.5F, 0.05D);
        for (int frame = 0; frame < 100; frame++) fast += atHundredFps.update(0.5F, 0.01D);
        assertEquals(slow, fast);
        assertEquals(11, slow);
    }

    @Test void releaseAndDirectionChangeEachProduceAResponsiveFirstStep() {
        ControllerAnalogScroll scroll = new ControllerAnalogScroll();
        assertEquals(1, scroll.update(0.2F, 0.0D));
        assertEquals(0, scroll.update(0.0F, 0.05D));
        assertEquals(-1, scroll.update(-0.2F, 0.0D));
    }

    @Test void invalidOrExcessiveFrameTimesCannotBurstTheQueue() {
        ControllerAnalogScroll scroll = new ControllerAnalogScroll();
        assertEquals(1, scroll.update(1.0F, 0.0D));
        assertEquals(0, scroll.update(1.0F, Double.NaN));
        assertEquals(1, scroll.update(1.0F, 10.0D));
    }
}
