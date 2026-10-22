package neofontrender.addons.inline;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SvgRasterizerTest {
    @Test
    void safeModeRendersStaticVectorContent() throws Exception {
        BufferedImage image = rasterize("<svg xmlns=\"http://www.w3.org/2000/svg\" "
                + "width=\"32\" height=\"20\"><rect width=\"32\" height=\"20\" "
                + "fill=\"#00ff00\"/></svg>", false);

        assertEquals(32, image.getWidth());
        assertEquals(20, image.getHeight());
        assertTrue((image.getRGB(10, 10) >>> 24) > 0);
    }

    @Test
    void safeModeRejectsCompatibilityFeatures() {
        assertThrows(IOException.class, () -> rasterize(svg("<style>rect{fill:red}</style>"), false));
        assertThrows(IOException.class, () -> rasterize(svg("<image href=\"data:image/png;base64,AA==\"/>"), false));
        assertThrows(IOException.class, () -> rasterize(svg("<animate attributeName=\"x\"/>"), false));
    }

    @Test
    void fullModeEnablesCssButStillRejectsActiveXmlContent() throws Exception {
        BufferedImage image = rasterize("<svg xmlns=\"http://www.w3.org/2000/svg\" "
                + "width=\"16\" height=\"16\"><style>.filled{fill:#ff0000}</style>"
                + "<rect class=\"filled\" width=\"16\" height=\"16\"/></svg>", true);
        assertTrue((image.getRGB(8, 8) >>> 24) > 0);

        assertThrows(IOException.class, () -> rasterize(svg("<script>alert(1)</script>"), true));
        assertThrows(IOException.class, () -> rasterize(svg("<foreignObject/>"), true));
        assertThrows(IOException.class, () -> rasterize(
                "<!DOCTYPE svg [<!ENTITY x SYSTEM \"file:///secret\">]>" + svg("<text>&x;</text>"), true));
    }

    @Test
    void fullModeRendersBudgetedEmbeddedImages() throws Exception {
        String onePixelPng = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwC"
                + "AAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";
        BufferedImage image = rasterize(svg("<image width=\"32\" height=\"32\" "
                + "href=\"data:image/png;base64," + onePixelPng + "\"/>"), true);
        assertEquals(32, image.getWidth());
        assertEquals(32, image.getHeight());
    }

    @Test
    void fullModeDoesNotLoadNestedExternalResources() throws Exception {
        BufferedImage image = rasterize(svg(
                "<image width=\"8\" height=\"8\" href=\"https://127.0.0.1:9/not-loaded.png\"/>"), true);
        assertEquals(32, image.getWidth());
        assertEquals(32, image.getHeight());
    }

    private static String svg(String body) {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"32\" height=\"32\">"
                + body + "</svg>";
    }

    private static BufferedImage rasterize(String value, boolean full) throws IOException {
        return SvgRasterizer.rasterize(value.getBytes(StandardCharsets.UTF_8), null, full, 128);
    }
}
