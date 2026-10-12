package neofontrender.addons.cursor;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorSvgRasterizerTest {
    @Test
    void rendersStaticSvgToArgb() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"32\" height=\"24\" "
                + "viewBox=\"0 0 32 24\"><path fill=\"#ff0000\" d=\"M0 0h20v20H0z\"/></svg>";
        BufferedImage image = CursorSvgRasterizer.rasterize(stream(svg));

        assertEquals(32, image.getWidth());
        assertEquals(24, image.getHeight());
        assertTrue((image.getRGB(5, 5) >>> 24) > 0);
    }

    @Test
    void rejectsExternalSvgResources() {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><image href=\"file:///x.png\"/></svg>";
        assertThrows(IOException.class, () -> CursorSvgRasterizer.rasterize(stream(svg)));
    }

    private static ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
