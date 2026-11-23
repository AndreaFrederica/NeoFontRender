package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TooltipSpacingTest {
    @Test
    void asymmetricPaddingExpandsThePanelInsteadOfMovingTheText() {
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            // Final glyph bounds already include the header's optical shift. The panel
            // must preserve that origin, with independently adjustable space on each side.
            TooltipConfig.leftPadding = 0;
            TooltipConfig.rightPadding = 24;
            TooltipConfig.topPadding = 2;
            TooltipConfig.bottomPadding = 7;
            TooltipConfig.Snapshot saved = TooltipConfig.snapshot();
            TooltipPanelBounds panel = TooltipPanelBounds.of(30, 40, 80, 3, 11);
            assertArrayEquals(new int[]{30, 41, 134, 58},
                    new int[]{panel.left, panel.top, panel.right, panel.bottom});
            TooltipConfig.topPadding = 6;
            TooltipConfig.bottomPadding = 0;
            panel = TooltipPanelBounds.of(30, 40, 80, 3, 11);
            assertArrayEquals(new int[]{30, 37, 134, 51},
                    new int[]{panel.left, panel.top, panel.right, panel.bottom});
            saved.restore();
            panel = TooltipPanelBounds.of(30, 40, 80, 3, 11);
            assertArrayEquals(new int[]{30, 41, 134, 58},
                    new int[]{panel.left, panel.top, panel.right, panel.bottom});
            assertArrayEquals(new int[]{3, 11}, TooltipLayout.roundedVisualBounds(3, 11));
        } finally {
            original.restore();
        }
    }

    @Test
    void dividerReservesItsOwnPixelAndBothMarginsOnlyWhenEnabled() {
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.titleBreak = true;
            TooltipConfig.dividerTopMargin = 4;
            TooltipConfig.dividerBottomMargin = 6;
            int headerBottom = 22;
            int dividerTop = headerBottom + TooltipConfig.dividerTopMargin;
            int bodyTop = headerBottom + TooltipLayout.dividerSpacing();
            assertEquals(4, dividerTop - headerBottom);
            assertEquals(6, bodyTop - (dividerTop + 1));
            TooltipConfig.titleBreak = false;
            assertEquals(0, TooltipLayout.dividerSpacing());
        } finally {
            original.restore();
        }
    }
}
