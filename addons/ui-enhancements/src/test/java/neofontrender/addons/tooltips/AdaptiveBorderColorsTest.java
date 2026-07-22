package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptiveBorderColorsTest {
    private static final int[] CONFIGURED = {
            0x80111111, 0x90222222, 0xA0333333, 0xB0444444
    };

    @Test
    void collectsEveryVisibleFormattingColorAndHonorsReset() {
        Set<Integer> colors = AdaptiveBorderColors.collectFormattedColors(
                "\u00A7cRed \u00A7bAqua \u00A7rBase", 0xFFFF55);
        assertEquals(Set.of(0xFF5555, 0x55FFFF, 0xFFFF55), colors);
    }

    @Test
    void oneColorProducesFourVariantsAndPreservesCornerAlpha() {
        AdaptiveBorderColors.Result result = AdaptiveBorderColors.synthesize(colors(0x55FFFF), true, CONFIGURED);
        assertFalse(result.spectrum);
        assertEquals(0x80000000, result.colors[0] & 0xFF000000);
        assertEquals(0x90000000, result.colors[1] & 0xFF000000);
        assertNotEquals(result.colors[0] & 0xFFFFFF, result.colors[2] & 0xFFFFFF);
    }

    @Test
    void twoThreeAndFourColorsProduceFourCornerPalettes() {
        assertFalse(AdaptiveBorderColors.synthesize(colors(0xFF0000, 0x00FF00), false, CONFIGURED).spectrum);
        assertFalse(AdaptiveBorderColors.synthesize(colors(0xFF0000, 0x00FF00, 0x0000FF), false, CONFIGURED).spectrum);
        assertFalse(AdaptiveBorderColors.synthesize(colors(0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00), false, CONFIGURED).spectrum);
    }

    @Test
    void moreThanFourColorsSelectsSpectrum() {
        AdaptiveBorderColors.Result result = AdaptiveBorderColors.synthesize(
                colors(1, 2, 3, 4, 5), false, CONFIGURED);
        assertTrue(result.spectrum);
        assertArrayEquals(CONFIGURED, result.colors);
    }

    private static Set<Integer> colors(int... values) {
        Set<Integer> result = new LinkedHashSet<>();
        for (int value : values) result.add(value);
        return result;
    }
}
