package neofontrender.addons.worldcreation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NfrColumnLayoutTest {
    @Test
    void preferredGapsAreUsedWhenSpaceIsPlentiful() {
        Map<String, Integer> rows = new NfrColumnLayout()
                .gap(10, 4).item("a", 20)
                .gap(20, 8).item("b", 20)
                .layout(100, 1000);

        assertEquals(110, rows.get("a"));
        assertEquals(150, rows.get("b"));
    }

    @Test
    void gapsShrinkToMinWhenSpaceIsTightAndRowsNeverOverlap() {
        Map<String, Integer> rows = new NfrColumnLayout()
                .gap(14, 8).item("a", 20)
                .gap(30, 6).item("b", 20)
                .gap(30, 6).item("c", 20)
                .layout(60, 60 + 8 + 20 + 6 + 20 + 6 + 20);

        assertEquals(68, rows.get("a"));
        assertEquals(94, rows.get("b"));
        assertEquals(120, rows.get("c"));
    }

    @Test
    void shortageIsDistributedProportionallyAcrossGaps() {
        // Two gaps 20..10 (weight 10) and 30..10 (weight 20); budget leaves 15 above the mins.
        Map<String, Integer> rows = new NfrColumnLayout()
                .gap(20, 10).item("a", 20)
                .gap(30, 10).item("b", 20)
                .layout(0, 35 + 40);

        int gapA = rows.get("a");
        int gapB = rows.get("b") - rows.get("a") - 20;
        assertEquals(35, gapA + gapB);
        assertTrue(gapA >= 10 && gapB >= 10, "gaps must respect their minimum");
        assertTrue(gapA <= 20 && gapB <= 30, "gaps must not exceed their preferred size");
    }

    @Test
    void rowsStayOrderedAndNonOverlappingEvenWhenOverflowing() {
        NfrColumnLayout column = new NfrColumnLayout()
                .gap(10, 4).item("a", 20)
                .gap(10, 4).item("b", 20)
                .gap(10, 4).item("c", 20);
        Map<String, Integer> rows = column.layout(0, 20);

        assertTrue(rows.get("a") < rows.get("b"));
        assertTrue(rows.get("b") < rows.get("c"));
        assertEquals(rows.get("a") + 20 + 4, (int) rows.get("b"));
        assertEquals(rows.get("b") + 20 + 4, (int) rows.get("c"));
    }
}
