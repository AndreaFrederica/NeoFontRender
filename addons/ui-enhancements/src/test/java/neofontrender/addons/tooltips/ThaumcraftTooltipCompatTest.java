package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ThaumcraftTooltipCompatTest {
    @Test
    void decodesThaumcraftCompactLineMarkers() {
        assertEquals(Arrays.asList("title", "body", "@inside"),
                ThaumcraftTooltipCompat.sanitizeLines(
                        Arrays.asList("title", "@@body", "@@@inside")));
    }

    @Test
    void handlesMissingTooltipLines() {
        assertEquals(Collections.emptyList(), ThaumcraftTooltipCompat.sanitizeLines(null));
        assertEquals(Collections.emptyList(), ThaumcraftTooltipCompat.sanitizeLines(
                Collections.emptyList()));
    }

    @Test
    void preservesCompactLineMetadataWithoutLeakingMarkers() {
        assertArrayEquals(new boolean[]{false, true, true, false},
                ThaumcraftTooltipCompat.compactLineFlags(
                        Arrays.asList("title", "@@small", "@@", "body")));
        assertEquals(Arrays.asList("title", "small", "", "body"),
                ThaumcraftTooltipCompat.sanitizeLines(
                        Arrays.asList("title", "@@small", "@@", "body")));
    }
}
