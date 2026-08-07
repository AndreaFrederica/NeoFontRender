package neofontrender.core.font.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ShadowColorPolicyTest {
    @Test
    void coloredLegacyShadowPreservesArgb() {
        assertEquals(0x7FE02D5F,
                ShadowColorPolicy.legacyColor(0x7FE02D5F, true));
    }

    @Test
    void defaultLegacyShadowUsesVanillaQuarterBrightness() {
        assertEquals(0x7F380B17,
                ShadowColorPolicy.legacyColor(0x7FE02D5F, false));
    }

    @Test
    void coloredPaletteShadowUsesForegroundEntry() {
        assertEquals(5, ShadowColorPolicy.paletteIndex(5, true, true));
        assertEquals(21, ShadowColorPolicy.paletteIndex(5, true, false));
        assertEquals(5, ShadowColorPolicy.paletteIndex(5, false, false));
    }

    @Test
    void modernShadowSelectsRunOrConfiguredColor() {
        assertEquals(0xFFE02D5F,
                ShadowColorPolicy.modernColor(0xFFE02D5F, 0xFF112233, true));
        assertEquals(0xFF112233,
                ShadowColorPolicy.modernColor(0xFFE02D5F, 0xFF112233, false));
    }

    @Test
    void coloredModernShadowAppliesConfiguredRemap() {
        ShadowColorRemapRules rules = ShadowColorRemapRules.parse("rgb:FFFFFF=000000");
        assertEquals(0x7F000000, ShadowColorPolicy.modernColor(
                0x7FFFFFFF, 0xFF112233, true, rules, null));
        assertEquals(0xFF112233, ShadowColorPolicy.modernColor(
                0x7FFFFFFF, 0xFF112233, false, rules, null));
    }
}
