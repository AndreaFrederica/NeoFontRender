package neofontrender.splash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SplashTextureAtlasLayoutTest {

    @Test
    void packsRegionsWithTransparentGuttersAndStartsNewRows() {
        SplashTextureAtlasLayout layout = new SplashTextureAtlasLayout(16, 12, 1);

        SplashTextureAtlasLayout.Placement first = layout.allocate(4, 3);
        SplashTextureAtlasLayout.Placement second = layout.allocate(6, 2);
        SplashTextureAtlasLayout.Placement third = layout.allocate(8, 2);

        assertNotNull(first);
        assertEquals(1, first.x);
        assertEquals(1, first.y);
        assertNotNull(second);
        assertEquals(7, second.x);
        assertEquals(1, second.y);
        assertNotNull(third);
        assertEquals(1, third.x);
        assertEquals(6, third.y);
    }

    @Test
    void resetReusesTheSameAtlasCoordinates() {
        SplashTextureAtlasLayout layout = new SplashTextureAtlasLayout(16, 12, 1);
        layout.allocate(4, 3);
        layout.allocate(6, 2);

        layout.reset();
        SplashTextureAtlasLayout.Placement placement = layout.allocate(4, 3);

        assertNotNull(placement);
        assertEquals(1, placement.x);
        assertEquals(1, placement.y);
    }

    @Test
    void rejectsRegionsThatCannotFitWithoutAllocatingAnotherAtlas() {
        SplashTextureAtlasLayout layout = new SplashTextureAtlasLayout(8, 8, 1);

        assertNull(layout.allocate(7, 2));
        assertNotNull(layout.allocate(6, 6));
        assertNull(layout.allocate(1, 1));
    }
}
