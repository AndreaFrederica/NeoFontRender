package neofontrender.addons.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyntheticMouseInputTest {
    @Test void nativeCoordinatesRoundTripThroughVanillaGuiScaling() {
        assertRoundTrip(0, 0, 427, 240, 1920, 1080);
        assertRoundTrip(213, 120, 427, 240, 1920, 1080);
        assertRoundTrip(426, 239, 427, 240, 1920, 1080);
        assertRoundTrip(37, 83, 320, 180, 2560, 1440);
    }

    private static void assertRoundTrip(int x, int y, int width, int height,
                                        int displayWidth, int displayHeight) {
        int eventX = SyntheticMouseCoordinates.nativeEventX(x, width, displayWidth);
        int eventY = SyntheticMouseCoordinates.nativeEventY(y, height, displayHeight);
        assertEquals(x, eventX * width / displayWidth);
        assertEquals(y, height - eventY * height / displayHeight - 1);
    }
}
