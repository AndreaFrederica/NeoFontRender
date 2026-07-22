package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TooltipConfigTest {
    @Test
    void parsesArgbAndRgbColors() {
        assertEquals(0x7F123456, TooltipConfig.parseColor("#7F123456", 0));
        assertEquals(0xFF123456, TooltipConfig.parseColor("123456", 0));
        assertEquals(0xCAFEBABE, TooltipConfig.parseColor("bad color", 0xCAFEBABE));
    }
}
