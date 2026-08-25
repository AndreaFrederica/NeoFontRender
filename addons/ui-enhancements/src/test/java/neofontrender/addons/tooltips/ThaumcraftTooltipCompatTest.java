package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
