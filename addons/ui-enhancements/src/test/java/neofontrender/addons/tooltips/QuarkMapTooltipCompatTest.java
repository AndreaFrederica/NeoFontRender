package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuarkMapTooltipCompatTest {
    @Test
    void placesPreviewAboveTheTooltipWhenThereIsRoom() {
        QuarkMapTooltipLayout.Placement placement =
                QuarkMapTooltipLayout.placeForTooltip(320, 240, 20, 100, 60, 5, 5);

        assertEquals(15, placement.x);
        assertEquals(20, placement.y);
        assertEquals(3, 95 - (placement.y + QuarkMapTooltipLayout.PANEL_SIZE));
    }

    @Test
    void movesPreviewToTheRightNearTheTopEdge() {
        QuarkMapTooltipLayout.Placement placement =
                QuarkMapTooltipLayout.placeForTooltip(320, 240, 20, 10, 60, 5, 5);

        assertEquals(88, placement.x);
        assertEquals(5, placement.y);
    }

    @Test
    void movesPreviewToTheLeftWhenTheRightEdgeIsOccupied() {
        QuarkMapTooltipLayout.Placement placement =
                QuarkMapTooltipLayout.placeForTooltip(320, 240, 250, 10, 60, 5, 5);

        assertEquals(170, placement.x);
        assertEquals(5, placement.y);
    }

    @Test
    void keepsAnAbovePreviewInsideTheHorizontalScreenMargins() {
        QuarkMapTooltipLayout.Placement placement =
                QuarkMapTooltipLayout.placeForTooltip(320, 240, 310, 100, 20, 5, 5);

        assertEquals(244, placement.x);
        assertEquals(20, placement.y);
    }

    @Test
    void preservesThePanelGapWithCustomVerticalPadding() {
        int tooltipY = 110;
        int verticalPadding = 9;
        QuarkMapTooltipLayout.Placement placement =
                QuarkMapTooltipLayout.placeForTooltip(
                        320, 240, 20, tooltipY, 60, 5, verticalPadding);

        int tooltipTop = tooltipY - verticalPadding;
        int previewBottom = placement.y + QuarkMapTooltipLayout.PANEL_SIZE;
        assertEquals(QuarkMapTooltipLayout.PANEL_GAP, tooltipTop - previewBottom);
    }
}
