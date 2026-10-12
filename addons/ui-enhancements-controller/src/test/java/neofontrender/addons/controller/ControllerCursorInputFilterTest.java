package neofontrender.addons.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerCursorInputFilterTest {
    @Test void appliesResponseToVectorLengthWithoutChangingDirection() {
        ControllerCursorInputFilter filter = new ControllerCursorInputFilter();
        filter.update(0.3F, 0.4F, 0.0D, 0.0F);

        assertEquals(0.075F, filter.x(), 1.0E-5F);
        assertEquals(0.100F, filter.y(), 1.0E-5F);
    }

    @Test void smoothingIsIndependentOfRenderFrameRate() {
        ControllerCursorInputFilter slow = new ControllerCursorInputFilter();
        ControllerCursorInputFilter fast = new ControllerCursorInputFilter();
        slow.update(0.2F, 0.0F, 0.0D, 0.5F);
        fast.update(0.2F, 0.0F, 0.0D, 0.5F);
        for (int frame = 0; frame < 20; frame++) slow.update(0.8F, 0.0F, 0.05D, 0.5F);
        for (int frame = 0; frame < 100; frame++) fast.update(0.8F, 0.0F, 0.01D, 0.5F);

        assertEquals(slow.x(), fast.x(), 1.0E-4F);
    }

    @Test void steadyInputDoesNotFeedEasedOutputBackIntoTheFilter() {
        ControllerCursorInputFilter filter = new ControllerCursorInputFilter();
        filter.update(0.5F, 0.0F, 0.0D, 1.0F);
        float first = filter.x();
        filter.update(0.5F, 0.0F, 0.016D, 1.0F);

        assertEquals(0.125F, first, 1.0E-6F);
        assertEquals(first, filter.x(), 1.0E-6F);
    }

    @Test void neutralInputStopsImmediatelyInsteadOfDrifting() {
        ControllerCursorInputFilter filter = new ControllerCursorInputFilter();
        filter.update(0.8F, 0.2F, 0.0D, 1.0F);
        filter.update(0.0F, 0.0F, 0.016D, 1.0F);

        assertEquals(0.0F, filter.x());
        assertEquals(0.0F, filter.y());
    }

    @Test void clampsDiagonalMagnitudeBeforeApplyingCurve() {
        ControllerCursorInputFilter filter = new ControllerCursorInputFilter();
        filter.update(1.0F, 1.0F, 0.0D, 0.0F);
        double length = Math.sqrt(filter.x() * filter.x() + filter.y() * filter.y());
        assertTrue(length <= 1.00001D);
    }
}
