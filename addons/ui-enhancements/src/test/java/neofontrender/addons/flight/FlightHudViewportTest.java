package neofontrender.addons.flight;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightHudViewportTest {
    @Test
    void virtualCanvasFitsAndRecentersAcrossWindowShapes() {
        for (int[] viewport : new int[][] {{1920, 1080}, {1280, 1024}, {2560, 1080}}) {
            Rectangle mapped = FlightHudViewport.fit(
                    viewport[0], viewport[1], 600, 340, 100);
            assertTrue(mapped.x >= 0 && mapped.y >= 0);
            assertTrue(mapped.x + mapped.width <= viewport[0]);
            assertTrue(mapped.y + mapped.height <= viewport[1]);
            assertEquals(viewport[0] / 2.0D, mapped.getCenterX(), 1.0D);
            assertEquals(viewport[1] / 2.0D, mapped.getCenterY(), 1.0D);
            assertEquals(600.0D / 340.0D,
                    mapped.width / (double) mapped.height, 0.01D);
        }
    }

    @Test
    void userScaleChangesMappedViewportWithoutEscapingSafeFit() {
        Rectangle full = FlightHudViewport.fit(1920, 1080, 540, 300, 100);
        Rectangle half = FlightHudViewport.fit(1920, 1080, 540, 300, 50);
        Rectangle oversized = FlightHudViewport.fit(1920, 1080, 540, 300, 200);
        assertEquals(full.width, oversized.width);
        assertEquals(full.height, oversized.height);
        assertEquals(full.width * 0.5D, half.width, 1.0D);
        assertEquals(full.height * 0.5D, half.height, 1.0D);
    }
}
