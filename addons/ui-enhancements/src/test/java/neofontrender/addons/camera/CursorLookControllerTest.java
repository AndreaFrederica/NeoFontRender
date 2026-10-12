package neofontrender.addons.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorLookControllerTest {
    @Test
    void startsAtTheScreenCenter() {
        CursorLookController cursor = new CursorLookController();
        cursor.reset(1920, 1080);

        assertEquals(960.0D, cursor.x(), 1.0E-9D);
        assertEquals(540.0D, cursor.y(), 1.0E-9D);
    }

    @Test
    void convertsLwjglUpwardYIntoScreenDownwardY() {
        CursorLookController cursor = new CursorLookController();
        cursor.reset(100, 100);
        cursor.update(100, 100, 10, 12, 1.0D);

        assertEquals(60.0D, cursor.x(), 1.0E-9D);
        assertEquals(38.0D, cursor.y(), 1.0E-9D);
    }

    @Test
    void clampsAtViewportEdges() {
        CursorLookController cursor = new CursorLookController();
        cursor.reset(100, 80);
        cursor.update(100, 80, 1000, -1000, 1.0D);

        assertEquals(99.0D, cursor.x(), 1.0E-9D);
        assertEquals(79.0D, cursor.y(), 1.0E-9D);
    }

    @Test
    void resizesAndResetsWhenTheFramebufferChanges() {
        CursorLookController cursor = new CursorLookController();
        cursor.reset(100, 80);
        cursor.update(200, 160, 0, 0, 1.0D);

        assertEquals(100.0D, cursor.x(), 1.0E-9D);
        assertEquals(80.0D, cursor.y(), 1.0E-9D);
    }

    @Test
    void controlTargetTogglesWithoutResettingCursorPosition() {
        CursorLookController cursor = new CursorLookController();
        cursor.reset(100, 80);
        cursor.update(100, 80, 12, -7, 1.0D);

        assertFalse(cursor.controlsCamera());
        cursor.toggleControlTarget();
        assertTrue(cursor.controlsCamera());
        assertEquals(62.0D, cursor.x(), 1.0E-9D);
        assertEquals(47.0D, cursor.y(), 1.0E-9D);

        cursor.toggleControlTarget();
        assertFalse(cursor.controlsCamera());
    }
}
