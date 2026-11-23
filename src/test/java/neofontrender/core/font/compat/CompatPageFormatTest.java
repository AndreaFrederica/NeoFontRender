package neofontrender.core.font.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the page-format detection: {@code auto} must only choose the swizzled {@code GL_R8}
 * layout when the context can express it, and the legacy format must remain reachable.
 */
class CompatPageFormatTest {

    @Test
    void autoFollowsTheContextCapability() {
        assertEquals(CompatPageFormat.R8_SWIZZLE,
                CompatPageFormat.select("auto", true));
        assertEquals(CompatPageFormat.ALPHA,
                CompatPageFormat.select("auto", false));
        assertEquals(CompatPageFormat.R8_SWIZZLE,
                CompatPageFormat.select(null, true));
        assertEquals(CompatPageFormat.ALPHA,
                CompatPageFormat.select(null, false));
    }

    @Test
    void explicitAlphaStaysLegacy() {
        assertEquals(CompatPageFormat.ALPHA,
                CompatPageFormat.select("alpha", true));
        assertEquals(CompatPageFormat.ALPHA,
                CompatPageFormat.select(" ALPHA ", false));
    }

    @Test
    void swizzleFallsBackWithoutOpenGL33() {
        assertEquals(CompatPageFormat.R8_SWIZZLE,
                CompatPageFormat.select("swizzle", true));
        assertEquals(CompatPageFormat.R8_SWIZZLE,
                CompatPageFormat.select("r8", true));
        assertEquals(CompatPageFormat.ALPHA,
                CompatPageFormat.select("swizzle", false));
    }

    @Test
    void unknownValuesBehaveLikeAuto() {
        assertEquals(CompatPageFormat.R8_SWIZZLE,
                CompatPageFormat.select("bogus", true));
        assertEquals(CompatPageFormat.ALPHA,
                CompatPageFormat.select("bogus", false));
    }
}
