package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemZoomLayoutTest {
    @Test
    void sizeCanGrowBeyondInventoryHeightWhenScreenHasRoom() {
        ItemZoomLayout small = ItemZoomLayout.fit("left", 750, 1600, 900,
                700, 350, 176, 166, 144, 8, 0);
        ItemZoomLayout large = ItemZoomLayout.fit("left", 750, 1600, 900,
                700, 350, 176, 166, 320, 8, 0);
        assertNotNull(small);
        assertNotNull(large);
        assertEquals(144, small.size);
        assertEquals(320, large.size);
        assertEquals(433, large.y + large.size / 2);
        assertTrue(large.x + large.size < 700);
    }

    @Test
    void maximumIs512AndExcessiveValuesAreClamped() {
        for (int requested : new int[]{512, 900}) {
            ItemZoomLayout layout = ItemZoomLayout.fit("left", 1000, 2000, 1200,
                    900, 500, 176, 166, requested, 8, 0);
            assertNotNull(layout);
            assertEquals(512, layout.size);
        }
    }

    @Test
    void narrowScreenLimitsSizeAndReservesPanelInset() {
        ItemZoomLayout layout = ItemZoomLayout.fit("left", 350, 800, 600,
                312, 217, 176, 166, 512, 8, 24);
        assertNotNull(layout);
        assertEquals(256, layout.size);
        assertTrue(layout.x - 24 >= 0);
        assertTrue(layout.x + layout.size + 24 <= 304);
        assertTrue(layout.y - 24 >= 0);
        assertTrue(layout.y + layout.size + 24 <= 600);
    }

    @Test
    void offCentreInventoryCannotPushPreviewPastScreenEdge() {
        ItemZoomLayout layout = ItemZoomLayout.fit("right", 750, 1600, 400,
                700, 10, 176, 166, 512, 8, 0);
        assertNotNull(layout);
        assertEquals(384, layout.size);
        assertEquals(8, layout.y);
        assertTrue(layout.x >= 884);
    }

    @Test
    void choosesUsableSideAndSkipsWhenNeitherSideFits() {
        ItemZoomLayout layout = ItemZoomLayout.fit("left", 75, 800, 600,
                20, 200, 176, 166, 320, 8, 0);
        assertNotNull(layout);
        assertEquals(320, layout.size);
        assertTrue(layout.x > 196);
        assertNull(ItemZoomLayout.fit("auto", 90, 200, 200,
                12, 17, 176, 166, 144, 8, 0));
    }
}
