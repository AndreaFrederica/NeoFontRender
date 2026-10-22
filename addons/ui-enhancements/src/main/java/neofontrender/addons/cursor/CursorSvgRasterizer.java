package neofontrender.addons.cursor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import neofontrender.addons.inline.SvgRasterizer;

/** Sandboxed static SVG-to-ARGB adapter kept separate from the cursor service API. */
public final class CursorSvgRasterizer {
    private CursorSvgRasterizer() {}

    public static BufferedImage rasterize(InputStream input) throws IOException {
        return SvgRasterizer.rasterize(input, false, 128);
    }
}
