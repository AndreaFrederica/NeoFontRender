package neofontrender.core.font.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ShadowColorPolicyTest {
    @Test
    void coloredModeDarkensEachRunWhilePreservingArgb() {
        assertEquals(0x7F380B17,
                ShadowColorPolicy.shadowColor(0x7FE02D5F, ShadowColorPolicy.COLORED,
                        0xFF000000, null, null));
    }

    @Test
    void defaultLegacyShadowUsesVanillaQuarterBrightness() {
        assertEquals(0x7F380B17,
                ShadowColorPolicy.shadowColor(0x7FE02D5F, ShadowColorPolicy.VANILLA,
                        0xFF000000, null, null));
    }

    @Test
    void coloredPaletteShadowUsesForegroundEntry() {
        assertEquals(5, ShadowColorPolicy.paletteIndex(5, true, ShadowColorPolicy.COLORED));
        assertEquals(21, ShadowColorPolicy.paletteIndex(5, true, ShadowColorPolicy.VANILLA));
        assertEquals(5, ShadowColorPolicy.paletteIndex(5, false, ShadowColorPolicy.VANILLA));
    }

    @Test
    void modernShadowSelectsRunOrConfiguredColor() {
        assertEquals(0xFF380B17,
                ShadowColorPolicy.modernColor(0xFFE02D5F, 0xFF112233,
                        ShadowColorPolicy.COLORED, null, null));
        assertEquals(0xFF112233,
                ShadowColorPolicy.modernColor(0xFFE02D5F, 0xFF112233,
                        ShadowColorPolicy.SOLID, null, null));
    }

    @Test
    void coloredModernShadowAppliesConfiguredRemap() {
        ShadowColorRemapRules rules = ShadowColorRemapRules.parse("rgb:FFFFFF=000000");
        assertEquals(0x7F000000, ShadowColorPolicy.modernColor(
                0x7FFFFFFF, 0xFF112233, ShadowColorPolicy.COLORED, rules, null));
        assertEquals(0xFF000000, ShadowColorPolicy.modernColor(
                0x7FFFFFFF, 0xFF112233, ShadowColorPolicy.SOLID, rules, null));
    }

    @Test
    void solidModeUsesConfiguredColorAndPreservesOverrideAlpha() {
        assertEquals(0xFF112233, ShadowColorPolicy.shadowColor(
                0x7FE02D5F, ShadowColorPolicy.SOLID, 0xFF112233, null, null));
    }

    @Test
    void overrideMatchesForegroundAndChangesCandidateShadow() {
        ShadowColorRemapRules rules = ShadowColorRemapRules.parse("rgb:E02D5F=010203");
        assertEquals(0x7F010203, ShadowColorPolicy.shadowColor(
                0x7FE02D5F, ShadowColorPolicy.COLORED, 0xFF112233, rules, null));
    }

    @Test
    void coloredRatioUsesConfiguredSrgbMultiplier() {
        assertEquals(0xFF704020, ShadowColorPolicy.darken(
                0xFFE08040, 0.5F, ShadowColorPolicy.COLORED_FUNCTION_SRGB));
    }

    @Test
    void linearLightFunctionProducesASeparatelyEncodedResult() {
        assertEquals(0xFFA45C2C, ShadowColorPolicy.darken(
                0xFFE08040, 0.5F, ShadowColorPolicy.COLORED_FUNCTION_LINEAR_LIGHT));
    }
}
