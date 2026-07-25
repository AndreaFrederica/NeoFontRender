package neofontrender.core.font.cosmic;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Standalone visual diagnostic for proving which byte-backed face Cosmic/Swash rasterizes.
 *
 * <p>This deliberately runs outside Minecraft and writes one Cosmic and one Java2D PNG per
 * style. The two rasterizers will not be pixel-identical, but the glyph outlines must match.</p>
 */
public final class CosmicFontComparison {
    private static final int RASTER_MAGIC = 0x434F534D;
    private static final String SAMPLE = "Chakra Petch 0123456789 AaGgQqMW";

    private CosmicFontComparison() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            throw new IllegalArgumentException(
                    "usage: CosmicFontComparison <output-dir> <regular.ttf> <bold.ttf> "
                            + "<italic.ttf> <bold-italic.ttf>");
        }
        CosmicRuntimeSupport.Compatibility compatibility = CosmicRuntimeSupport.ensureLoaded();
        if (!compatibility.isSupported()) {
            throw new IllegalStateException(compatibility.getMessage());
        }

        Path output = Path.of(args[0]).toAbsolutePath();
        Files.createDirectories(output);
        Path[] paths = new Path[4];
        byte[][] fonts = new byte[4][];
        String[] aliases = new String[4];
        for (int i = 0; i < 4; i++) {
            paths[i] = Path.of(args[i + 1]).toAbsolutePath();
            fonts[i] = Files.readAllBytes(paths[i]);
            aliases[i] = paths[i].getFileName().toString();
        }

        long engine = CosmicNative.createEngine(fonts, aliases, "Chakra Petch",
                new String[0], "", "", "", "", false, 0,
                8.0F, Locale.getDefault().toLanguageTag());
        if (engine == 0L) {
            throw new IllegalStateException("cosmic-text returned a null engine");
        }
        try {
            System.out.println("Cosmic family: " + CosmicNative.primaryFamily(engine));
            String[] styleNames = {"regular", "bold", "italic", "bold-italic"};
            for (int style = 0; style < 4; style++) {
                String face = CosmicNative.resolvedFace(engine, style);
                // Exercise the same explicit-size route used by the UIE loading title: the engine
                // default remains 8px while this call directly requests a native 32px face at 1x.
                byte[] raster = CosmicNative.renderSized(
                        engine, SAMPLE, 0xFFFFFFFF, style, 32.0F, 1.0F);
                Path cosmicPng = output.resolve("cosmic-" + styleNames[style] + ".png");
                writeCosmicPng(raster, cosmicPng);

                Path javaPng = output.resolve("java2d-" + styleNames[style] + ".png");
                writeJava2dPng(paths[style], javaPng);
                System.out.println(styleNames[style] + ": " + face
                        + " -> " + cosmicPng + " | " + javaPng);
            }
            String warnings = CosmicNative.resolutionWarnings(engine);
            if (warnings != null && !warnings.isEmpty()) {
                System.out.println("Warnings:\n" + warnings);
            }
        } finally {
            CosmicNative.destroyEngine(engine);
        }
    }

    private static void writeCosmicPng(byte[] encoded, Path output) throws Exception {
        ByteBuffer data = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        if (data.remaining() < 32 || data.getInt() != RASTER_MAGIC) {
            throw new IllegalStateException("invalid Cosmic raster");
        }
        int width = data.getInt();
        int height = data.getInt();
        data.position(32);
        if (width <= 0 || height <= 0 || data.remaining() != width * height * 4) {
            throw new IllegalStateException("invalid Cosmic dimensions " + width + "x" + height);
        }
        BufferedImage glyphs = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        int[] pixels = ((DataBufferInt) glyphs.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = data.getInt();
        }

        BufferedImage image = darkCanvas(width + 32, height + 32);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.drawImage(glyphs, 16, 16, null);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", output.toFile());
    }

    private static void writeJava2dPng(Path fontPath, Path output) throws Exception {
        Font font = Font.createFont(Font.TRUETYPE_FONT, fontPath.toFile()).deriveFont(32.0F);
        BufferedImage measureImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measure = measureImage.createGraphics();
        configureJava2d(measure);
        measure.setFont(font);
        FontMetrics metrics = measure.getFontMetrics();
        int width = metrics.stringWidth(SAMPLE);
        int height = metrics.getHeight();
        int ascent = metrics.getAscent();
        measure.dispose();

        BufferedImage image = darkCanvas(width + 32, height + 32);
        Graphics2D graphics = image.createGraphics();
        try {
            configureJava2d(graphics);
            graphics.setFont(font);
            graphics.setColor(Color.WHITE);
            graphics.drawString(SAMPLE, 16, 16 + ascent);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", output.toFile());
    }

    private static BufferedImage darkCanvas(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(20, 22, 26));
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void configureJava2d(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }
}
