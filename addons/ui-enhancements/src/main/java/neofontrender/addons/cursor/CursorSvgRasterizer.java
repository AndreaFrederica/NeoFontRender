package neofontrender.addons.cursor;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Sandboxed static SVG-to-ARGB adapter kept separate from the cursor service API. */
final class CursorSvgRasterizer {
    private static final int MAX_INPUT_BYTES = 512 * 1024;
    private static final int MAX_RASTER_SIZE = 128;
    private static final int DEFAULT_SIZE = 32;
    private static final String[] FORBIDDEN = {
            "<!doctype", "<!entity", "<script", "<foreignobject", "<image",
            "xlink:href", "href=", "url(", "@import"
    };

    private CursorSvgRasterizer() {}

    static BufferedImage rasterize(InputStream input) throws IOException {
        byte[] bytes = readLimited(input);
        String text = new String(bytes, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        for (String token : FORBIDDEN) {
            if (text.contains(token)) throw new IOException("Unsupported external or active SVG content: " + token);
        }

        SVGDocument document = new SVGLoader().load(new ByteArrayInputStream(bytes));
        if (document == null) throw new IOException("SVG document could not be parsed");
        if (document.isAnimated()) throw new IOException("Animated SVG cursors are not supported");
        FloatSize size = document.size();
        ViewBox source = document.viewBox();
        double sourceWidth = positive(size == null ? 0.0D : size.getWidth(),
                source == null ? 0.0D : source.getWidth(), DEFAULT_SIZE);
        double sourceHeight = positive(size == null ? 0.0D : size.getHeight(),
                source == null ? 0.0D : source.getHeight(), DEFAULT_SIZE);
        double scale = Math.min(1.0D,
                Math.min(MAX_RASTER_SIZE / sourceWidth, MAX_RASTER_SIZE / sourceHeight));
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.scale(width / sourceWidth, height / sourceHeight);
        document.render(null, graphics,
                source == null ? new ViewBox((float) sourceWidth, (float) sourceHeight) : source);
        graphics.dispose();
        return image;
    }

    private static byte[] readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            total += read;
            if (total > MAX_INPUT_BYTES) throw new IOException("SVG exceeds 512 KiB limit");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static double positive(double first, double second, double fallback) {
        if (Double.isFinite(first) && first > 0.0D) return first;
        if (Double.isFinite(second) && second > 0.0D) return second;
        return fallback;
    }
}
