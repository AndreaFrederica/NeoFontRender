package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NfrTooltipAnchorTest {
    @Test
    void recognizesGuiScreenColorPrefixAndPreservesAnchorPosition() {
        String marker = "<nfr:anchor family=\"items\" producer=\"ae2\"/>";
        for (String decorated : Arrays.asList("\u00a77" + marker,
                "\u00a7r\u00a77" + marker, " \u00a77" + marker + "\u00a7r ")) {
            assertTrue(NfrTooltipAnchor.isAnchorLine(decorated));
            NfrTooltipAnchor anchor = NfrTooltipAnchor.parse(decorated);
            assertNotNull(anchor);
            assertEquals("items", anchor.family);
            assertEquals("ae2", anchor.producer);
            NfrTooltipAnchor.FamilyScan scan = NfrTooltipAnchor.scanFamily(
                    Arrays.asList("title", decorated, "mod name"), "items");
            assertEquals(1, scan.firstIndex);
            assertEquals(1, scan.count);
        }
        assertFalse(NfrTooltipAnchor.isAnchorLine("\u00a77description " + marker));
        assertNull(NfrTooltipAnchor.parse("\u00a77" + marker + " description"));
    }

    @Test
    void parsesCanonicalMarkerWithAllAttributes() {
        NfrTooltipAnchor anchor = NfrTooltipAnchor.parse(
                "<nfr:anchor family=\"items\" producer=\"ae2\" custom=\"storage-cell\"/>");

        assertNotNull(anchor);
        assertEquals("items", anchor.family);
        assertEquals("ae2", anchor.producer);
        assertEquals("storage-cell", anchor.custom);
        assertTrue(anchor.unknownAttributes.isEmpty());
    }

    @Test
    void acceptsPlainClosingAndKeepsUnknownAttributes() {
        NfrTooltipAnchor anchor = NfrTooltipAnchor.parse(
                "<nfr:anchor family=\"text\" producer=\"mod_x\" future=\"v2\">");

        assertNotNull(anchor);
        assertEquals("text", anchor.family);
        assertNull(anchor.custom);
        assertEquals("v2", anchor.unknownAttributes.get("future"));
    }

    @Test
    void rejectsMalformedAndForeignLines() {
        assertFalse(NfrTooltipAnchor.isAnchorLine("按下 R 查看内容"));
        assertNull(NfrTooltipAnchor.parse("按下 R 查看内容"));
        assertNull(NfrTooltipAnchor.parse("<nfr:anchor family=\"items\"/>"));
        assertNull(NfrTooltipAnchor.parse("<nfr:anchor family=\"items\" producer=\"ae2\""));
        assertNull(NfrTooltipAnchor.parse("<nfr:anchor family=items producer=\"ae2\"/>"));
    }

    @Test
    void foldsReservedBlockIntoOneMarkerAtTheSamePosition() {
        List<String> lines = new ArrayList<>(Arrays.asList(
                "256k 便携元件", "\u00a70\u00a7r    ", "\u00a70\u00a7r    ", "按下 R 查看内容"));

        List<String> folded = NfrTooltipAnchor.foldReservedBlocks(lines,
                line -> line.startsWith("\u00a70\u00a7r"), "items", "ae2");

        assertEquals(Arrays.asList("256k 便携元件",
                "<nfr:anchor family=\"items\" producer=\"ae2\"/>", "按下 R 查看内容"), folded);
    }

    @Test
    void foldLeavesDocumentsWithoutReservedLinesUntouched() {
        List<String> lines = Arrays.asList("a", "b");

        assertSame(lines, NfrTooltipAnchor.foldReservedBlocks(lines,
                line -> line.startsWith("\u00a70\u00a7r"), "items", "ae2"));
    }

    @Test
    void scanPicksFirstAnchorOfTheFamilyAndCountsDuplicates() {
        List<String> lines = Arrays.asList(
                "<nfr:anchor family=\"text\" producer=\"other\"/>",
                "<nfr:anchor family=\"items\" producer=\"ae2\"/>",
                "<nfr:anchor family=\"items\" producer=\"third_party\"/>");

        NfrTooltipAnchor.FamilyScan scan = NfrTooltipAnchor.scanFamily(lines, "items");

        assertTrue(scan.found());
        assertEquals(1, scan.firstIndex);
        assertEquals(2, scan.count);
        assertEquals(new LinkedHashSet<>(Arrays.asList("ae2", "third_party")), scan.producers);
        assertFalse(NfrTooltipAnchor.scanFamily(lines, "chart").found());
    }
}
