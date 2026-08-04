package neofontrender.core.font.support;

import neofontrender.api.color.TextColorPaletteRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShadowColorRemapRulesTest {
    @Test
    void exactRgbRulePreservesAlpha() {
        ShadowColorRemapRules rules = ShadowColorRemapRules.parse("rgb:FFFFFF=000000");
        assertEquals(0x7F000000, rules.remap(0x7FFFFFFF, null));
        assertEquals(0xFF55FF55, rules.remap(0xFF55FF55, null));
    }

    @Test
    void paletteSlotFollowsResolvedPalette() {
        int[] palette = TextColorPaletteRegistry.vanillaColorCodes();
        ShadowColorRemapRules rules = ShadowColorRemapRules.parse("slot:e=6A5200");
        assertEquals(0xFF6A5200, rules.remap(0xFF000000 | palette[14], palette));
    }

    @Test
    void invalidRulesAreDiscardedAndValidRulesAreCanonicalized() {
        ShadowColorRemapRules rules = ShadowColorRemapRules.parse(
                "broken; #ffffff > #010203; slot:F=0x000000");
        assertEquals("rgb:FFFFFF=010203;slot:f=000000", rules.toConfigString());
        assertTrue(ShadowColorRemapRules.parse("broken").isEmpty());
    }
}
