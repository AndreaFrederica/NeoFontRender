package neofontrender.addons.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerVirtualCursorTest {
    @Test
    void preservesPositionAcrossScreensAndClampsOnlyWhenNeeded() {
        ControllerVirtualCursor cursor = new ControllerVirtualCursor();
        assertFalse(cursor.isInitialized());

        cursor.set(70.0D, 60.0D, 100, 80);
        cursor.resize(120, 90);
        assertTrue(cursor.isInitialized());
        assertEquals(70, cursor.x());
        assertEquals(60, cursor.y());

        cursor.resize(50, 40);
        assertEquals(49, cursor.x());
        assertEquals(39, cursor.y());
        assertEquals(49, cursor.renderX(0.0F));
        assertEquals(39, cursor.renderY(0.0F));
    }

    @Test
    void integratesSignedAxesAndClampsToScreen() {
        ControllerVirtualCursor cursor = new ControllerVirtualCursor();
        cursor.set(50.0D, 40.0D, 100, 80);

        assertTrue(cursor.move(1.0F, -1.0F, 100, 80));
        assertEquals(59, cursor.x());
        assertEquals(31, cursor.y());

        for (int i = 0; i < 20; i++) cursor.move(1.0F, -1.0F, 100, 80);
        assertEquals(99, cursor.x());
        assertEquals(0, cursor.y());
    }

    @Test
    void neutralInputDoesNotMoveCursor() {
        ControllerVirtualCursor cursor = new ControllerVirtualCursor();
        cursor.set(12.0D, 14.0D, 100, 80);
        assertFalse(cursor.move(0.0F, 0.0F, 100, 80));
        assertEquals(12, cursor.x());
        assertEquals(14, cursor.y());
    }

    @Test
    void interpolatesBetweenPreviousAndCurrentPositions() {
        ControllerVirtualCursor cursor = new ControllerVirtualCursor();
        cursor.set(10.0D, 20.0D, 200, 120);
        cursor.move(1.0F, 0.0F, 200, 120);

        assertEquals(10, cursor.renderX(0.0F));
        assertEquals(15, cursor.renderX(0.5F));
        assertEquals(19, cursor.renderX(1.0F));
        assertEquals(20, cursor.renderY(0.5F));
    }

    @Test
    void invalidPartialTicksAreClamped() {
        ControllerVirtualCursor cursor = new ControllerVirtualCursor();
        cursor.set(10.0D, 20.0D, 200, 120);
        cursor.move(1.0F, 0.0F, 200, 120);

        assertEquals(10, cursor.renderX(Float.NaN));
        assertEquals(19, cursor.renderX(2.0F));
        assertEquals(10, cursor.renderX(-1.0F));
    }

    @Test
    void customSpeedAndAttractionRemainBounded() {
        ControllerVirtualCursor cursor = new ControllerVirtualCursor();
        cursor.set(10.0D, 10.0D, 40, 30);
        cursor.move(1.0F, 0.0F, 4.0D, 40, 30);
        assertEquals(14, cursor.x());

        cursor.attract(30.0D, 20.0D, 0.5D, 40, 30);
        assertEquals(22, cursor.x());
        assertEquals(15, cursor.y());
        cursor.attract(100.0D, 100.0D, 1.0D, 40, 30);
        assertEquals(39, cursor.x());
        assertEquals(29, cursor.y());
    }

    @Test
    void retainsSubpixelMovementForRendering() {
        ControllerVirtualCursor cursor = new ControllerVirtualCursor();
        cursor.set(10.0D, 10.0D, 100, 80);
        cursor.move(0.1F, 0.0F, 1.0D, 100, 80);

        assertTrue(cursor.xDouble() > 10.0D);
        assertTrue(cursor.xDouble() < 11.0D);
        assertEquals(cursor.xDouble(), cursor.renderXDouble(1.0F), 1.0E-9D);
    }

    @Test
    void integratesAnAlreadyProcessedVectorWithoutChangingItsDirection() {
        ControllerVirtualCursor cursor = new ControllerVirtualCursor();
        cursor.set(10.0D, 10.0D, 100, 80);
        cursor.move(0.3F, 0.4F, 10.0D, 100, 80);

        assertEquals(13.0D, cursor.xDouble(), 1.0E-6D);
        assertEquals(14.0D, cursor.yDouble(), 1.0E-6D);
    }
}
