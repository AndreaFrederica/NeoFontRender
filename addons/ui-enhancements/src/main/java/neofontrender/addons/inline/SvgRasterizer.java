package neofontrender.addons.inline;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.DomDocument;
import com.github.weisj.jsvg.parser.DocumentLimits;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.resources.RenderableResource;
import com.github.weisj.jsvg.parser.resources.ResourceSupplier;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.parser.resources.impl.ImageResource;
import com.github.weisj.jsvg.parser.resources.impl.ValueResourceSupplier;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/** Sandboxed JSVG 2.x adapter shared by cursor assets and inline vector content. */
public final class SvgRasterizer {
    private static final int MAX_INPUT_BYTES = 8 * 1024 * 1024;
    private static final String[] ALWAYS_FORBIDDEN = {
            "<!doctype", "<!entity", "<script", "<foreignobject"
    };
    private static final String[] SAFE_MODE_FORBIDDEN = {
            "<image", "<style", "<animate", "<set", "@import"
    };

    private SvgRasterizer() {}

    public static BufferedImage rasterize(InputStream input, boolean full, int maximumDimension)
            throws IOException {
        return rasterize(readLimited(input), null, full, maximumDimension);
    }

    static BufferedImage rasterize(byte[] bytes, URI baseUri, boolean full, int maximumDimension)
            throws IOException {
        validateSource(bytes, full);
        LoaderContext.Builder contextBuilder = LoaderContext.builder()
                .externalResourcePolicy(full ? ResourcePolicy.DENY_EXTERNAL : ResourcePolicy.DENY_ALL)
                .documentLimits(new DocumentLimits(full ? 40 : 30, full ? 20 : 15,
                        full ? 4000 : 2000));
        if (full) contextBuilder.resourceLoader(SvgRasterizer::loadEmbeddedImage);
        LoaderContext context = contextBuilder.build();
        SVGDocument document = new SVGLoader().load(new ByteArrayInputStream(bytes), baseUri, context);
        if (document == null) throw new IOException("SVG document could not be parsed");
        if (!full && document.isAnimated()) {
            throw new IOException("Animated SVG requires full compatibility mode");
        }

        FloatSize size = document.size();
        ViewBox source = document.viewBox();
        double sourceWidth = positive(size == null ? 0.0D : size.getWidth(),
                source == null ? 0.0D : source.getWidth(), 32.0D);
        double sourceHeight = positive(size == null ? 0.0D : size.getHeight(),
                source == null ? 0.0D : source.getHeight(), 32.0D);
        int max = Math.max(16, Math.min(2048, maximumDimension));
        double scale = Math.min(1.0D, Math.min(max / sourceWidth, max / sourceHeight));
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        if ((long) width * height > 4L * 1024L * 1024L) {
            throw new IOException("SVG raster exceeds pixel budget");
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            document.render(null, graphics, new ViewBox(0, 0, width, height));
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void validateSource(byte[] bytes, boolean full) throws IOException {
        if (bytes == null || bytes.length == 0) throw new IOException("SVG is empty");
        if (bytes.length > MAX_INPUT_BYTES) throw new IOException("SVG exceeds 8 MiB limit");
        String text = new String(bytes, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        for (String token : ALWAYS_FORBIDDEN) {
            if (text.contains(token)) throw new IOException("Unsupported active SVG content: " + token);
        }
        if (!full) {
            for (String token : SAFE_MODE_FORBIDDEN) {
                if (text.contains(token)) {
                    throw new IOException("SVG feature requires full compatibility mode: " + token);
                }
            }
        }
    }

    private static byte[] readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            total += read;
            if (total > MAX_INPUT_BYTES) throw new IOException("SVG exceeds 8 MiB limit");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static ResourceSupplier<RenderableResource> loadEmbeddedImage(
            DomDocument document, URI uri) throws IOException {
        if (uri == null || !"data".equalsIgnoreCase(uri.getScheme())) return null;
        String value = uri.toASCIIString();
        int comma = value.indexOf(',');
        if (comma < 0) throw new IOException("Malformed embedded SVG image");
        String metadata = value.substring(5, comma).toLowerCase(Locale.ROOT);
        if (!metadata.startsWith("image/") || !metadata.endsWith(";base64")) {
            throw new IOException("Embedded SVG images must use base64 image data");
        }
        byte[] decoded;
        try {
            decoded = Base64.getMimeDecoder().decode(value.substring(comma + 1));
        } catch (IllegalArgumentException invalidData) {
            throw new IOException("Malformed embedded SVG image", invalidData);
        }
        if (decoded.length > MAX_INPUT_BYTES) {
            throw new IOException("Embedded SVG image exceeds 8 MiB limit");
        }
        return new ValueResourceSupplier<>(new ImageResource(
                InlineImageService.decodeRaster(decoded)));
    }

    private static double positive(double first, double second, double fallback) {
        if (Double.isFinite(first) && first > 0.0D) return first;
        if (Double.isFinite(second) && second > 0.0D) return second;
        return fallback;
    }
}
