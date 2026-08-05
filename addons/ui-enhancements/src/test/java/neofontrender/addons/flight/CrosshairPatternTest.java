package neofontrender.addons.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosshairPatternTest {
    @Test
    void ignoresMalformedAndOutOfBoundsCoordinates() {
        boolean[][] pixels = CrosshairPattern.parse("0,0;2,3;bad;99,1;-1,2;1,no", 5);

        assertTrue(pixels[0][0]);
        assertTrue(pixels[2][3]);
        assertFalse(pixels[1][2]);
    }

    @Test
    void serializationRoundTripsInStableColumnMajorOrder() {
        boolean[][] pixels = new boolean[4][4];
        pixels[3][0] = true;
        pixels[1][2] = true;

        String encoded = CrosshairPattern.serialize(pixels);
        assertEquals("1,2;3,0", encoded);
        assertEquals(encoded, CrosshairPattern.serialize(CrosshairPattern.parse(encoded, 4)));
    }
}
