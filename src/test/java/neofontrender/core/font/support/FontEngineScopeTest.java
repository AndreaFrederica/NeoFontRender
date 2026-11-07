package neofontrender.core.font.support;

import org.junit.jupiter.api.Test;

import static neofontrender.core.font.support.FontEngineScope.Mode.*;
import static org.junit.jupiter.api.Assertions.*;

class FontEngineScopeTest {
    @Test
    void debugMeasurementsAndDrawingRestoreTheEngineBeforeOtherUiRuns() {
        for (var mode : new FontEngineScope.Mode[] {VANILLA, COSMIC, COMPAT}) {
            assertEquals(INHERIT, FontEngineScope.current());
            int width = FontEngineScope.callForOverlay(true, mode, () -> {
                assertEquals(mode, FontEngineScope.current());
                return 73;
            });
            assertEquals(73, width);
            assertEquals(INHERIT, FontEngineScope.current(), "HUD event listeners keep their engine");
            int endX = FontEngineScope.callForOverlay(true, mode, () -> {
                assertEquals(mode, FontEngineScope.current());
                return 2 + width;
            });
            assertEquals(75, endX);
            assertEquals(INHERIT, FontEngineScope.current(), "Chat and screens keep their engine");
        }
    }

    @Test
    void hiddenDebugOverlayAndInheritDoNotOverrideTheCaller() {
        FontEngineScope.run(COSMIC, () -> {
            for (var mode : FontEngineScope.Mode.values()) {
                FontEngineScope.callForOverlay(false, mode, () -> {
                    assertEquals(COSMIC, FontEngineScope.current());
                    return 0;
                });
            }
            FontEngineScope.callForOverlay(true, INHERIT, () -> {
                assertEquals(COSMIC, FontEngineScope.current());
                return 0;
            });
        });
        assertEquals(INHERIT, FontEngineScope.current());
    }

    @Test
    void nestedOverlayCallsAndExceptionsRestoreEachCaller() {
        var failure = new IllegalStateException("font draw failed");
        FontEngineScope.run(COMPAT, () -> {
            assertSame(failure, assertThrows(IllegalStateException.class, () ->
                    FontEngineScope.callForOverlay(true, VANILLA, () -> {
                        FontEngineScope.callForOverlay(true, COSMIC, () -> {
                            assertEquals(COSMIC, FontEngineScope.current());
                            return 1;
                        });
                        assertEquals(VANILLA, FontEngineScope.current());
                        throw failure;
                    })));
            assertEquals(COMPAT, FontEngineScope.current());
        });
        assertEquals(INHERIT, FontEngineScope.current());
        assertThrows(IllegalStateException.class, () ->
                FontEngineScope.callForOverlay(true, VANILLA, () -> { throw failure; }));
        assertEquals(INHERIT, FontEngineScope.current());
    }

    @Test
    void anotherThreadDoesNotInheritTheOverlayEngine() throws Exception {
        var observed = new java.util.concurrent.atomic.AtomicReference<FontEngineScope.Mode>();
        FontEngineScope.callForOverlay(true, VANILLA, () -> {
            Thread thread = new Thread(() -> observed.set(FontEngineScope.current()));
            thread.start();
            try {
                thread.join(5000);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new AssertionError(error);
            }
            assertFalse(thread.isAlive());
            assertEquals(VANILLA, FontEngineScope.current());
            return 0;
        });
        assertEquals(INHERIT, observed.get());
        assertEquals(INHERIT, FontEngineScope.current());
    }

    @Test
    void overlayRestoresSharedWidthsIncludingRepeatedAndUnicodeOnlyGlyphs() {
        int[] ascii = {6, 7};
        byte[] unicode = {12, 14, 16};
        for (var mode : new FontEngineScope.Mode[] {VANILLA, COMPAT}) {
            FontEngineScope.callForOverlay(true, mode, () -> {
                assertTrue(FontEngineScope.preserveMetrics(ascii, 0, unicode, 0));
                ascii[0] = 9;
                unicode[0] = 18;
                assertTrue(FontEngineScope.preserveMetrics(ascii, 0, unicode, 0));
                ascii[0] = 10;
                unicode[0] = 20;
                assertTrue(FontEngineScope.preserveMetrics(ascii, -1, unicode, 2));
                unicode[2] = 22;
                return 0;
            });
            assertArrayEquals(new int[] {6, 7}, ascii, "Later UI keeps its original ASCII spacing");
            assertArrayEquals(new byte[] {12, 14, 16}, unicode, "Unicode widths are restored too");
        }
        assertFalse(FontEngineScope.preserveMetrics(ascii, 0, unicode, 0),
                "The global compat engine retains its normal persistent width updates");
    }

    @Test
    void failedNestedFontCallRestoresWidthsToEachEnclosingScope() {
        int[] ascii = {6};
        byte[] unicode = {12};
        FontEngineScope.run(COMPAT, () -> {
            FontEngineScope.preserveMetrics(ascii, 0, unicode, 0);
            ascii[0] = 9;
            unicode[0] = 18;
            assertThrows(IllegalStateException.class, () ->
                    FontEngineScope.callForOverlay(true, VANILLA, () -> {
                        FontEngineScope.preserveMetrics(ascii, 0, unicode, 0);
                        ascii[0] = 6;
                        unicode[0] = 12;
                        throw new IllegalStateException("font draw failed");
                    }));
            assertArrayEquals(new int[] {9}, ascii);
            assertArrayEquals(new byte[] {18}, unicode);
        });
        assertArrayEquals(new int[] {6}, ascii);
        assertArrayEquals(new byte[] {12}, unicode);
    }
}
