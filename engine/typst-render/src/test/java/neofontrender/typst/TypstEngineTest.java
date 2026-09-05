package neofontrender.typst;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypstEngineTest {
    @TempDir Path temporaryDirectory;

    @Test
    void rendersStraightAlphaArgbWithoutAnImageCodec() throws Exception {
        try (TypstEngine engine = TypstEngine.open(temporaryDirectory)) {
            TypstRaster raster = engine.render(
                    "#set page(width: auto, height: auto, margin: 0pt, fill: none)\n"
                            + "$ integral_0^1 x^2 dif x $", 2.0F);
            assertTrue(raster.width() > 0);
            assertTrue(raster.height() > 0);
            assertEquals(raster.width() * raster.height(), raster.argb().length);
            boolean visible = false;
            boolean transparent = false;
            for (int pixel : raster.argb()) {
                visible |= (pixel >>> 24) != 0;
                transparent |= (pixel >>> 24) == 0;
            }
            assertTrue(visible, "formula must contain visible pixels");
            assertTrue(transparent, "transparent page background must survive JNI");
        }
    }

    @Test
    void rejectsUseAfterClose() throws Exception {
        TypstEngine engine = TypstEngine.open(temporaryDirectory);
        engine.close();
        assertThrows(IllegalStateException.class, () -> engine.render("test", 1.0F));
    }
}
