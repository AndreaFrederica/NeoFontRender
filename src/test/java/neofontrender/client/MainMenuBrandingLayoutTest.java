package neofontrender.client;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class MainMenuBrandingLayoutTest {
    @Test void hiddenBrandingLeavesNoPhantomRows() {
        MainMenuBrandingLayout.Position p = place(Collections.emptyList());
        assertEquals(2, p.x);
        assertEquals(229, p.y);
    }

    @Test void stacksAboveAllVisibleBrandingRows() {
        MainMenuBrandingLayout.Position p = place(Arrays.asList(
                box(2, 230, 100, 9), box(2, 220, 100, 9), box(2, 210, 100, 9)));
        assertEquals(2, p.x);
        assertEquals(199, p.y);
    }

    @Test void followsMovedCmmLabelsAndTheirScaledBounds() {
        MainMenuBrandingLayout.Position p = place(Collections.singletonList(box(24, 180, 160, 36)));
        assertEquals(24, p.x);
        assertEquals(169, p.y);
    }

    @Test void rightCopyrightDoesNotReserveLeftRowsUnlessItOverlaps() {
        assertEquals(229, place(Collections.singletonList(box(220, 228, 178, 10))).y);
        assertEquals(217, place(Collections.singletonList(box(60, 228, 338, 10))).y);
    }

    @Test void emptyAndOffscreenRowsAreIgnored() {
        MainMenuBrandingLayout.Position p = place(Arrays.asList(
                box(2, 230, 0, 9), box(2, 250, 100, 9)));
        assertEquals(229, p.y);
    }

    @Test void avoidsClippingWhenTheScreenIsTooSmall() {
        assertNull(MainMenuBrandingLayout.place(50, 20, 90, 9, Collections.emptyList()));
        assertNull(MainMenuBrandingLayout.place(100, 20, 90, 9,
                Collections.singletonList(box(0, 0, 100, 20))));
    }

    private static MainMenuBrandingLayout.Position place(java.util.List<MainMenuBrandingLayout.Bounds> bounds) {
        return MainMenuBrandingLayout.place(400, 240, 90, 9, bounds);
    }

    private static MainMenuBrandingLayout.Bounds box(int x, int y, int width, int height) {
        return new MainMenuBrandingLayout.Bounds(x, y, width, height);
    }
}
