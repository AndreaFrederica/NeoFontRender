package neofontrender.addons.inline;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;
import neofontrender.core.config.NeofontrenderConfig;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Pure AWT adapter from a TeX formula to a transparent high-resolution raster. */
final class LatexRasterizer {
    private static final float SOURCE_SIZE = 28.0F;
    private static final int MAX_SOURCE_DIMENSION = 4096;
    private static final long MAX_SOURCE_PIXELS = 8L * 1024L * 1024L;
    private static volatile String registeredFontSignature = "";

    private LatexRasterizer() {}

    static BufferedImage rasterize(String formula) {
        return rasterize(formula, 1.0F);
    }

    static BufferedImage rasterize(String formula, float oversample) {
        registerTextFont(EmbeddedContentConfig.latexFontFamily());
        float rasterSize = SOURCE_SIZE * Math.max(1.0F, Math.min(8.0F, oversample));
        TeXIcon icon = new TeXFormula(formula).createTeXIcon(
                TeXConstants.STYLE_DISPLAY, rasterSize);
        icon.setInsets(new Insets(2, 2, 2, 2));
        icon.setForeground(Color.WHITE);
        int width = Math.max(1, icon.getIconWidth());
        int height = Math.max(1, icon.getIconHeight());
        if (width > MAX_SOURCE_DIMENSION || height > MAX_SOURCE_DIMENSION
                || (long) width * height > MAX_SOURCE_PIXELS) {
            throw new IllegalArgumentException("LaTeX raster exceeds size budget");
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static synchronized void registerTextFont(String configuredFamily) {
        String family = configuredFamily == null ? "" : configuredFamily.trim();
        List<String> candidates = new ArrayList<>();
        if (!family.isEmpty()) {
            candidates.add(family);
        } else {
            // Fira Math is shipped with this addon and is the stable default for formulas.
            candidates.add(EmbeddedContentFonts.FIRA_MATH_LOCATION);
            try {
                // Keep the same primary/fallback/resource ordering as the core font pipeline.
                candidates.addAll(NeofontrenderConfig.fontFamily());
            } catch (Throwable ignored) {
                // Early test/startup rendering can happen before the core config is loaded.
            }
        }
        List<String> resolved = new ArrayList<>();
        for (String candidate : candidates) {
            String resolvedFamily = resolveFamily(candidate);
            if (!resolvedFamily.isEmpty() && !resolved.contains(resolvedFamily)) {
                resolved.add(resolvedFamily);
            }
        }
        if (resolved.isEmpty()) return;
        String primary = resolved.get(0);
        String fallback = resolved.size() > 1 ? resolved.get(1) : "Serif";
        String signature = primary + "\u0000" + fallback;
        if (signature.equals(registeredFontSignature)) return;
        org.scilab.forge.jlatexmath.TeXFormula.registerExternalFont(
                Character.UnicodeBlock.BASIC_LATIN, primary, fallback);
        registeredFontSignature = signature;
    }

    private static String resolveFamily(String candidate) {
        String value = candidate == null ? "" : candidate.trim();
        if (value.isEmpty()) return "";
        try {
            Font font = loadFont(value);
            if (font != null) {
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                return font.getFamily();
            }
            Font installed = new Font(value, Font.PLAIN, 1);
            if (!"Dialog".equalsIgnoreCase(installed.getFamily())
                    || "Dialog".equalsIgnoreCase(value)) {
                return installed.getFamily();
            }
        } catch (Throwable ignored) {
            // A bad optional family must leave the normal AWT fallback chain intact.
        }
        return "";
    }

    private static Font loadFont(String candidate) {
        File file = new File(candidate);
        if (file.isFile()) {
            try {
                return Font.createFont(Font.TRUETYPE_FONT, file);
            } catch (Throwable ignored) {
                return null;
            }
        }
        String resource = candidate;
        int separator = resource.indexOf(':');
        if (separator > 0 && separator < resource.length() - 1) {
            resource = "assets/" + resource.substring(0, separator) + "/"
                    + resource.substring(separator + 1);
        }
        if (!resource.startsWith("assets/")) return null;
        try (InputStream stream = LatexRasterizer.class.getClassLoader()
                .getResourceAsStream(resource)) {
            return stream == null ? null : Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
