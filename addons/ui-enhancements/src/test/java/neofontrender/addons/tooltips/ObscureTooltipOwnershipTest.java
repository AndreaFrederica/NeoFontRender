package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObscureTooltipOwnershipTest {
    @Test
    void eitherPreviewSwitchSelectsTheEntireUieRenderer() {
        assertTrue(ObscureTooltipCompat.selectsNfrRenderer(true, true, true, false));
        assertTrue(ObscureTooltipCompat.selectsNfrRenderer(true, true, false, true));
        assertTrue(ObscureTooltipCompat.selectsNfrRenderer(true, true, true, true));
        assertFalse(ObscureTooltipCompat.selectsNfrRenderer(true, true, false, false));
        assertFalse(ObscureTooltipCompat.selectsNfrRenderer(false, true, true, true));
        assertFalse(ObscureTooltipCompat.selectsNfrRenderer(true, false, true, true));
    }

    @Test
    void titleIconAlsoKeepsOwnershipWithUie() {
        assertTrue(ObscureTooltipCompat.selectsNfrRenderer(true, true, false, false, true));
        assertFalse(ObscureTooltipCompat.selectsNfrRenderer(true, true, false, false, false));
    }
}
