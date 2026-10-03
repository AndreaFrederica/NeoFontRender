package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TooltipDebugLabelTest {
    @Test
    void clipsLongLabelsWithAProgressBoundedSearch() {
        assertEquals("12...", ModernTooltipRenderer.clipDebugLabel(
                "123456789", 5, String::length));
        assertEquals("short", ModernTooltipRenderer.clipDebugLabel(
                "short", 5, String::length));
        assertEquals("...", ModernTooltipRenderer.clipDebugLabel(
                "long", 2, String::length));
    }
}
