package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import static neofontrender.addons.tooltips.ItemZoomDrawOrder.Phase.AFTER_SCREEN;
import static neofontrender.addons.tooltips.ItemZoomDrawOrder.Phase.BEFORE_TOOLTIP;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemZoomDrawOrderTest {
    @Test
    void tooltipOnTopDrawsOnceAcrossForgeAndDocumentCallbacks() {
        ItemZoomDrawOrder order = new ItemZoomDrawOrder();
        order.beginFrame();
        assertTrue(order.claim("below_tooltip", BEFORE_TOOLTIP)); // Forge Pre
        assertFalse(order.claim("below_tooltip", BEFORE_TOOLTIP)); // Modern / HEI bridge
        assertFalse(order.claim("below_tooltip", AFTER_SCREEN));
    }

    @Test
    void previewOnTopWaitsForScreenPost() {
        ItemZoomDrawOrder order = new ItemZoomDrawOrder();
        order.beginFrame();
        assertFalse(order.claim("above_tooltip", BEFORE_TOOLTIP));
        assertFalse(order.claim("above_tooltip", BEFORE_TOOLTIP));
        assertTrue(order.claim("above_tooltip", AFTER_SCREEN));
        assertFalse(order.claim("above_tooltip", AFTER_SCREEN));
    }

    @Test
    void belowTooltipNeverFallsBackToDrawingOverFinishedTooltip() {
        ItemZoomDrawOrder order = new ItemZoomDrawOrder();
        order.beginFrame();
        assertFalse(order.claim("below_tooltip", AFTER_SCREEN));
    }

    @Test
    void layerCanChangeBetweenFramesWithoutDoubleDrawing() {
        ItemZoomDrawOrder order = new ItemZoomDrawOrder();
        assertTrue(order.claim("below_tooltip", BEFORE_TOOLTIP));
        assertFalse(order.claim("above_tooltip", AFTER_SCREEN));
        order.beginFrame();
        assertFalse(order.claim("above_tooltip", BEFORE_TOOLTIP));
        assertTrue(order.claim("above_tooltip", AFTER_SCREEN));
        order.beginFrame();
        assertTrue(order.claim("below_tooltip", BEFORE_TOOLTIP));
    }
}
