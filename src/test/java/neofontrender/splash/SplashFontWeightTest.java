package neofontrender.splash;

import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SplashFontWeightTest {

    @Test
    void inspectsWeightAxisFromSfntFvarTable() throws IOException {
        SplashFontWeight.WeightAxis axis = SplashFontWeight.inspect(
                sfntWithWeightAxis(100, 400, 900));

        assertEquals(100, axis.minimum);
        assertEquals(400, axis.defaultWeight);
        assertEquals(900, axis.maximum);
        assertEquals("fvar", axis.source);
    }

    @Test
    void rejectsTruncatedFvarTable() {
        byte[] font = sfntWithWeightAxis(100, 400, 900);
        ByteBuffer.wrap(font).order(ByteOrder.BIG_ENDIAN).putInt(24, 35);

        assertThrows(IOException.class, () -> SplashFontWeight.inspect(font));
    }

    @Test
    void resolvesVariableFontWeightByRemovingAwtBoldAndEmboldeningFromDefault() {
        SplashFontWeight.WeightAxis axis =
                new SplashFontWeight.WeightAxis(100, 400, 900, "fvar");

        SplashFontWeight.Resolution resolution = SplashFontWeight.resolve(
                axis, 650, Font.BOLD | Font.ITALIC, "Variable Test");

        assertEquals(Font.ITALIC, resolution.awtStyle);
        assertEquals(700, resolution.requestedTarget);
        assertEquals(700, resolution.appliedTarget);
        assertEquals(300, resolution.emboldenDelta);
        assertTrue(resolution.adjusted);
        assertFalse(resolution.clamped);
    }

    @Test
    void clampsRequestedWeightToVariableAxisMaximum() {
        SplashFontWeight.WeightAxis axis =
                new SplashFontWeight.WeightAxis(100, 400, 700, "fvar");

        SplashFontWeight.Resolution resolution = SplashFontWeight.resolve(
                axis, 900, Font.PLAIN, "Variable Test");

        assertEquals(900, resolution.requestedTarget);
        assertEquals(700, resolution.appliedTarget);
        assertEquals(300, resolution.emboldenDelta);
        assertTrue(resolution.clamped);
    }

    @Test
    void fallsBackWhenAwtCannotReduceVariableFontDefaultWeight() {
        SplashFontWeight.WeightAxis axis =
                new SplashFontWeight.WeightAxis(100, 700, 900, "fvar");

        assertThrows(SplashFontWeight.Fallback.class,
                () -> SplashFontWeight.resolve(axis, 400, Font.PLAIN, "Variable Test"));
    }

    @Test
    void leavesNonVariableFontStyleToAwt() {
        SplashFontWeight.Resolution resolution = SplashFontWeight.resolve(
                null, 500, Font.BOLD | Font.ITALIC, "Static Test");

        assertEquals(Font.BOLD | Font.ITALIC, resolution.awtStyle);
        assertEquals(700, resolution.requestedTarget);
        assertEquals(0, resolution.emboldenDelta);
        assertFalse(resolution.adjusted);
    }

    @Test
    void scalesAndCapsOutlineStrokeWidthAndPadding() {
        assertEquals(0.0F, SplashFontWeight.outlineStrokeWidth(16.0F, 0));
        assertEquals(0.6F, SplashFontWeight.outlineStrokeWidth(16.0F, 300), 0.0001F);
        assertEquals(2.0F, SplashFontWeight.outlineStrokeWidth(16.0F, 2000), 0.0001F);
        assertEquals(4, SplashAwtBackend.rasterPadding(0.0F));
        assertEquals(6, SplashAwtBackend.rasterPadding(0.6F));
        assertEquals(6, SplashAwtBackend.rasterPadding(2.0F));
    }

    @Test
    void selectsCoreBlendFunctionWhenOpenGl14IsAvailable() {
        assertEquals(SplashAwtBackend.BlendFunctionPath.CORE_14,
                SplashAwtBackend.selectBlendFunctionPath(true, true));
        assertTrue(SplashAwtBackend.BlendFunctionPath.CORE_14.separate);
    }

    @Test
    void selectsExtensionBlendFunctionWithoutOpenGl14() {
        assertEquals(SplashAwtBackend.BlendFunctionPath.EXTENSION,
                SplashAwtBackend.selectBlendFunctionPath(false, true));
        assertTrue(SplashAwtBackend.BlendFunctionPath.EXTENSION.separate);
    }

    @Test
    void selectsLegacyBlendFunctionWithoutSeparateBlendSupport() {
        assertEquals(SplashAwtBackend.BlendFunctionPath.LEGACY,
                SplashAwtBackend.selectBlendFunctionPath(false, false));
        assertFalse(SplashAwtBackend.BlendFunctionPath.LEGACY.separate);
    }

    private static byte[] sfntWithWeightAxis(int minimum, int defaultWeight, int maximum) {
        ByteBuffer font = ByteBuffer.allocate(64).order(ByteOrder.BIG_ENDIAN);
        font.putInt(0, 0x00010000);
        font.putShort(4, (short) 1);

        font.putInt(12, tag("fvar"));
        font.putInt(20, 28);
        font.putInt(24, 36);

        font.putShort(32, (short) 16);
        font.putShort(36, (short) 1);
        font.putShort(38, (short) 20);

        font.putInt(44, tag("wght"));
        font.putInt(48, fixed(minimum));
        font.putInt(52, fixed(defaultWeight));
        font.putInt(56, fixed(maximum));
        return font.array();
    }

    private static int fixed(int value) {
        return value << 16;
    }

    private static int tag(String value) {
        return (value.charAt(0) << 24) | (value.charAt(1) << 16)
                | (value.charAt(2) << 8) | value.charAt(3);
    }
}
