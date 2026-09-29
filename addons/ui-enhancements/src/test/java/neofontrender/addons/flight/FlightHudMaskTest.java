package neofontrender.addons.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlightHudMaskTest {
    @Test
    void opacityControlsAlphaAndColorPickerAlphaIsIgnored() {
        int originalColor = FlightRollConfig.hudMaskColor;
        int originalOpacity = FlightRollConfig.hudMaskOpacityPercent;
        try {
            FlightRollConfig.hudMaskColor = 0x12123456;
            FlightRollConfig.hudMaskOpacityPercent = 15;
            assertEquals(0x26123456, FlightRollConfig.hudMaskArgb());

            FlightRollConfig.hudMaskOpacityPercent = 100;
            assertEquals(0xFF123456, FlightRollConfig.hudMaskArgb());
        } finally {
            FlightRollConfig.hudMaskColor = originalColor;
            FlightRollConfig.hudMaskOpacityPercent = originalOpacity;
        }
    }
}
