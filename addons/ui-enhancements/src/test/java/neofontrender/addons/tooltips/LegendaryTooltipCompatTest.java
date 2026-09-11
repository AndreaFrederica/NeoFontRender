package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegendaryTooltipCompatTest {
    @Test
    void panelPriorityComposesBothModsWithoutDisablingLayout() {
        assertEquals(TooltipPanelOwner.NFR, TooltipPanelOwner.choose(false, false, true, true));
        assertEquals(TooltipPanelOwner.LEGENDARY, TooltipPanelOwner.choose(true, false, true, false));
        assertEquals(TooltipPanelOwner.LEGENDARY, TooltipPanelOwner.choose(true, true, true, true));
        assertEquals(TooltipPanelOwner.OBSCURE, TooltipPanelOwner.choose(true, true, false, true));
        assertEquals(TooltipPanelOwner.NFR, TooltipPanelOwner.choose(true, true, false, false));
        assertEquals(TooltipPanelOwner.OBSCURE, TooltipPanelOwner.choose(false, true, true, true));
    }

    @Test
    void decorationsUsePanelEdgesIncludingTheObscureHeader() {
        assertFalse(LegendaryTooltipCompat.hasLayout());
        try (LegendaryTooltipCompat.Scope ignored = LegendaryTooltipCompat.begin(20, 30, 220, 160)) {
            LegendaryTooltipCompat.Bounds bounds = LegendaryTooltipCompat.bounds();
            assertEquals(23, bounds.x);
            assertEquals(33, bounds.y);
            assertEquals(194, bounds.width);
            assertEquals(124, bounds.height);
            assertEquals(20, bounds.x - 3);
            assertEquals(160, bounds.y + bounds.height + 3);
        }
        assertFalse(LegendaryTooltipCompat.hasLayout());
    }

    @Test
    void nestedAndFailedRenderingRestoresOuterDecorationBounds() {
        try (LegendaryTooltipCompat.Scope outer = LegendaryTooltipCompat.begin(10, 10, 100, 100)) {
            LegendaryTooltipCompat.Bounds previous = LegendaryTooltipCompat.bounds();
            assertThrows(IllegalStateException.class, () -> {
                try (LegendaryTooltipCompat.Scope inner = LegendaryTooltipCompat.begin(0, 0, 20, 20)) {
                    throw new IllegalStateException("render failed");
                }
            });
            assertSame(previous, LegendaryTooltipCompat.bounds());
        }
        assertFalse(LegendaryTooltipCompat.hasLayout());
    }
}
