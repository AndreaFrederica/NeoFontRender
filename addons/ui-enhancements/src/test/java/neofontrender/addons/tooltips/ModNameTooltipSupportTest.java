package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModNameTooltipSupportTest {
    @Test
    void movesOwnershipLinePastIntegrationLines() {
        List<String> tooltip = new ArrayList<>(Arrays.asList(
                "256k 便携元件", "§7AE2 Supergiant", "按下 R 查看内容"));

        assertTrue(ModNameTooltipSupport.moveModNameToEnd(tooltip, "AE2 Supergiant"));

        assertEquals(Arrays.asList("256k 便携元件", "按下 R 查看内容", "§7AE2 Supergiant"), tooltip);
    }

    @Test
    void reportsOriginalOwnershipIndexForVisualAnchoring() {
        List<String> tooltip = Arrays.asList("256k 便携元件", "§7AE2 Supergiant", "按下 R 查看内容");

        assertEquals(1, ModNameTooltipSupport.indexOfModNameLine(tooltip, "AE2 Supergiant"));
        assertEquals(-1, ModNameTooltipSupport.indexOfModNameLine(tooltip, "AE2"));
    }

    @Test
    void leavesTooltipUntouchedWhenOwnershipIsAlreadyLast() {
        List<String> tooltip = new ArrayList<>(Arrays.asList(
                "256k 便携元件", "按下 R 查看内容", "§7AE2 Supergiant"));

        assertFalse(ModNameTooltipSupport.moveModNameToEnd(tooltip, "AE2 Supergiant"));

        assertEquals(Arrays.asList("256k 便携元件", "按下 R 查看内容", "§7AE2 Supergiant"), tooltip);
    }
}
