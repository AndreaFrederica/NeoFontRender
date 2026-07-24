package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipConfigTest {
    @Test
    void parsesArgbAndRgbColors() {
        assertEquals(0x7F123456, TooltipConfig.parseColor("#7F123456", 0));
        assertEquals(0xFF123456, TooltipConfig.parseColor("123456", 0));
        assertEquals(0xCAFEBABE, TooltipConfig.parseColor("bad color", 0xCAFEBABE));
    }

    @Test
    void formatsAndDeduplicatesModNames() {
        assertEquals("\u00a79\u00a7o", ModNameTooltipSupport.format("blue italic"));
        assertEquals("", ModNameTooltipSupport.format("unknown reset"));
        assertTrue(ModNameTooltipSupport.containsModName(
                Arrays.asList("Item", "\u00a79\u00a7oMinecraft"), "Minecraft"));
        assertTrue(ModNameTooltipSupport.containsModName(
                Arrays.asList("Item", "  \u00a79\u00a7oMinecraft  "), "Minecraft"));
        assertFalse(ModNameTooltipSupport.containsModName(
                Arrays.asList("Item", "minecraft:stone"), "Minecraft"));
    }
}
