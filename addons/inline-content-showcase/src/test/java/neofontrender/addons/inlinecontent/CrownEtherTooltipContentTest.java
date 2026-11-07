package neofontrender.addons.inlinecontent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrownEtherTooltipContentTest {
    @Test
    void compactStateKeepsTheTooltipDense() {
        List<String> lines = CrownEtherSample.lines(CrownEtherSample.Detail.COMPACT);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains(CrownEtherSample.FORMULA_TOKEN));
        assertFalse(lines.contains(CrownEtherSample.STRUCTURE_TOKEN));
        assertFalse(lines.contains(CrownEtherSample.SMILES));
    }

    @Test
    void expandedStateAddsStructureAndMolarMass() {
        List<String> lines = CrownEtherSample.lines(CrownEtherSample.Detail.EXPANDED);
        assertEquals(5, lines.size());
        assertTrue(lines.contains(CrownEtherSample.STRUCTURE_TOKEN));
        assertTrue(lines.stream().anyMatch(line -> line.contains(CrownEtherSample.MOLAR_MASS_TOKEN)));
        assertFalse(lines.contains(CrownEtherSample.SMILES));
        assertFalse(lines.contains(""));
    }

    @Test
    void detailedStateContainsTheCompleteChemicalRecord() {
        List<String> lines = CrownEtherSample.lines(CrownEtherSample.Detail.DETAILED);
        assertEquals(8, lines.size());
        assertTrue(lines.contains(CrownEtherSample.NAME));
        assertTrue(lines.contains(CrownEtherSample.SMILES));
        assertTrue(lines.contains(CrownEtherSample.STRUCTURE_TOKEN));
        assertTrue(lines.contains("ContentTweaker"));
        assertFalse(lines.contains(""));
    }
}
