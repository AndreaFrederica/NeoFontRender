package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TooltipPanelOwnerTest {
    @Test
    void panelPriorityOnlySelectsUieOrLegendary() {
        assertEquals(TooltipPanelOwner.NFR, TooltipPanelOwner.choose(false, false, "auto", true));
        assertEquals(TooltipPanelOwner.LEGENDARY, TooltipPanelOwner.choose(true, false, "auto", true));
        assertEquals(TooltipPanelOwner.NFR, TooltipPanelOwner.choose(true, false, "uie", true));
        assertEquals(TooltipPanelOwner.LEGENDARY, TooltipPanelOwner.choose(true, false, "legendary", false));
        assertEquals(TooltipPanelOwner.LEGENDARY, TooltipPanelOwner.choose(false, true, "auto", false));
        assertEquals(TooltipPanelOwner.NFR, TooltipPanelOwner.choose(false, false, null, true));
    }
}
