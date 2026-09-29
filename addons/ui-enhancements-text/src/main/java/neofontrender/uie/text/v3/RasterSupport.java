package neofontrender.uie.text.v3;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.DocumentLimits;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import neofontrender.text.InlineRaster;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RasterSupport {
    private static final int MAX_INPUT_BYTES = 8 * 1024 * 1024;
    private static final int MAX_DIMENSION = 2048;
    private static final long MAX_PIXELS = 4L * 1024L * 1024L;
    private static volatile String registeredFontSignature = "";

    private RasterSupport() {}

    static InlineRaster latex(String formula, float oversample) {
        return latex(formula, oversample, Collections.emptyList());
    }

    static InlineRaster latex(String formula, float oversample, List<String> fontSelectors) {
        registerTextFonts(fontSelectors);
        float size = 28.0F * Math.max(1.0F, Math.min(8.0F, oversample));
        TeXIcon icon = new TeXFormula(formula).createTeXIcon(TeXConstants.STYLE_DISPLAY, size);
        icon.setInsets(new Insets(2, 2, 2, 2));
        icon.setForeground(Color.WHITE);
        int width = Math.max(1, icon.getIconWidth());
        int height = Math.max(1, icon.getIconHeight());
        checkDimensions(width, height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }
        return fromImage(image);
    }

    private static synchronized void registerTextFonts(List<String> selectors) {
        List<String> resolved = new ArrayList<>();
        if (selectors != null) {
            for (String selector : selectors) {
                String family = resolveFamily(selector);
                if (!family.isEmpty() && !resolved.contains(family)) resolved.add(family);
            }
        }
        if (resolved.isEmpty()) return;
        String primary = resolved.get(0);
        String fallback = resolved.size() > 1 ? resolved.get(1) : "Serif";
        String signature = primary + '\u0000' + fallback;
        if (signature.equals(registeredFontSignature)) return;
        TeXFormula.registerExternalFont(Character.UnicodeBlock.BASIC_LATIN, primary, fallback);
        registeredFontSignature = signature;
    }

    static String resolveFamily(String selector) {
        String value = selector == null ? "" : selector.trim();
        if (value.isEmpty()) return "";
        try {
            Font font = loadFont(value);
            if (font != null) {
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                return font.getFamily();
            }
            Font installed = new Font(value, Font.PLAIN, 1);
            if (!"Dialog".equalsIgnoreCase(installed.getFamily())
                    || "Dialog".equalsIgnoreCase(value)) return installed.getFamily();
        } catch (Throwable ignored) {
            // Optional font selectors must not prevent formula rendering.
        }
        return "";
    }

    private static Font loadFont(String selector) {
        File file = new File(selector);
        if (file.isFile()) {
            try {
                return Font.createFont(Font.TRUETYPE_FONT, file);
            } catch (Throwable ignored) {
                return null;
            }
        }
        String resource = selector.replace('\\', '/');
        int separator = resource.indexOf(':');
        if (separator > 0 && separator < resource.length() - 1) {
            resource = "assets/" + resource.substring(0, separator) + "/"
                    + resource.substring(separator + 1);
        }
        while (resource.startsWith("/")) resource = resource.substring(1);
        if (!resource.startsWith("assets/")) return null;
        try (InputStream stream = RasterSupport.class.getClassLoader()
                .getResourceAsStream(resource)) {
            return stream == null ? null : Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static InlineRaster svg(byte[] bytes, URI baseUri, boolean full) throws IOException {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_INPUT_BYTES) {
            throw new IOException("SVG input is empty or exceeds 8 MiB");
        }
        String lower = new String(bytes, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        for (String forbidden : new String[]{"<!doctype", "<!entity", "<script", "<foreignobject"}) {
            if (lower.contains(forbidden)) throw new IOException("Unsafe SVG content: " + forbidden);
        }
        LoaderContext context = LoaderContext.builder()
                .externalResourcePolicy(ResourcePolicy.DENY_ALL)
                .documentLimits(new DocumentLimits(full ? 40 : 30, full ? 20 : 15,
                        full ? 4000 : 2000)).build();
        SVGDocument document = new SVGLoader().load(new ByteArrayInputStream(bytes), baseUri, context);
        if (document == null) throw new IOException("SVG could not be parsed");
        FloatSize size = document.size();
        ViewBox box = document.viewBox();
        double sourceWidth = positive(size == null ? 0 : size.getWidth(),
                box == null ? 0 : box.getWidth(), 32);
        double sourceHeight = positive(size == null ? 0 : size.getHeight(),
                box == null ? 0 : box.getHeight(), 32);
        double scale = Math.min(1.0, Math.min(MAX_DIMENSION / sourceWidth,
                MAX_DIMENSION / sourceHeight));
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        checkDimensions(width, height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            document.render(null, graphics, new ViewBox(0, 0, width, height));
        } finally {
            graphics.dispose();
        }
        return fromImage(image);
    }

    static InlineRaster image(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_INPUT_BYTES) {
            throw new IOException("Image input is empty or exceeds 8 MiB");
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) throw new IOException("Unsupported image format");
        checkDimensions(image.getWidth(), image.getHeight());
        return fromImage(image);
    }

    private static InlineRaster fromImage(BufferedImage image) {
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0,
                image.getWidth());
        return new InlineRaster(image.getWidth(), image.getHeight(), pixels);
    }

    private static void checkDimensions(int width, int height) {
        if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION
                || (long) width * height > MAX_PIXELS) {
            throw new IllegalArgumentException("Inline raster exceeds dimension budget");
        }
    }

    private static double positive(double first, double second, double fallback) {
        if (Double.isFinite(first) && first > 0) return first;
        if (Double.isFinite(second) && second > 0) return second;
        return fallback;
    }
}
