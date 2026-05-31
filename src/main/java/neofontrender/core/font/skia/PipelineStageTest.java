package neofontrender.core.font.skia;

import io.github.humbleui.skija.*;
import io.github.humbleui.skija.paragraph.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

/**
 * Offline pipeline stage test. Traces every processing step from Skia rendering
 * to the final int[] that goes into DynamicTexture, exporting a PNG at each stage.
 *
 * <p>Run standalone: {@code java neofontrender.core.font.skia.PipelineStageTest [outputDir]}</p>
 *
 * <p>This does NOT require Minecraft. It uses only Skija + AWT ImageIO.</p>
 *
 * <h3>Pipeline stages tested:</h3>
 * <pre>
 *   0  Raw Skia N32Premul (premultiplied BGRA as-is)
 *   1  After premultiplied→straight alpha conversion (what the mod does)
 *   2  After normalizeTransparentRgb (edge bleed fill)
 *   3  After 2x scaleNearest (textureScale)
 *   4  Wrong format: interpret ABGR int as ARGB (channel swap test)
 *   5  Skia's own PNG export (ground truth reference)
 * </pre>
 */
public final class PipelineStageTest {

    private static final String[] FAMILIES = {
            "Segoe UI", "Microsoft YaHei UI", "Noto Sans CJK SC",
            "Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji",
            "Sarasa UI SC", "SansSerif"
    };

    private PipelineStageTest() {}

    public static void main(String[] args) throws Exception {
        File outputDir = args.length > 0
                ? new File(args[0])
                : new File("build/pipeline-test");
        outputDir.mkdirs();
        System.out.println("Output: " + outputDir.getAbsolutePath());

        // --- Config ---
        float fontSize = 40.0f;
        int oversample = 8;   // 8x like the real mod
        String text = "Hello World 你好世界";

        // --- Build paragraph ---
        TextStyle style = new TextStyle()
                .setColor(0xFFFFFFFF)
                .setFontSize(fontSize * oversample)
                .setFontFamilies(FAMILIES)
                .setHeight(1.0F);
        ParagraphStyle ps = new ParagraphStyle();
        ps.setTextStyle(style);

        Paragraph paragraph;
        try (FontCollection fonts = new FontCollection()
                .setDefaultFontManager(FontMgr.getDefault())
                .setEnableFallback(true);
             ParagraphBuilder builder = new ParagraphBuilder(ps, fonts)) {
            builder.pushStyle(style);
            builder.addText(text);
            paragraph = builder.build();
        }
        paragraph.layout(100000.0F);

        int paraW = (int) Math.ceil(paragraph.getMaxIntrinsicWidth());
        int paraH = (int) Math.ceil(paragraph.getHeight());
        int border = Math.max(4, (int) (oversample * 8.0f / 16.0f) * 2);
        border += border % 2;
        border = Math.max(border, 4);

        int width = paraW + border * 2;
        int height = paraH + border * 2;
        System.out.printf("Canvas: %dx%d, fontSize=%.0f, oversample=%dx, border=%d%n",
                width, height, fontSize * oversample, oversample, border);

        // --- Stage 0: Raw Skia premultiplied output ---
        int[] rawPremul;
        try (Surface surface = Surface.makeRasterN32Premul(width, height)) {
            Canvas canvas = surface.getCanvas();
            canvas.clear(0x00000000);
            paragraph.paint(canvas, border, border);

            // Skia's own PNG export (ground truth)
            try (Image skiaImg = surface.makeImageSnapshot();
                 Data skiaPng = skiaImg.encodeToData(EncodedImageFormat.PNG)) {
                Files.write(new File(outputDir, "stage5_skia_ground_truth.png").toPath(),
                        skiaPng.getBytes());
            }

            // Read raw pixels as int[]
            Bitmap bitmap = new Bitmap();
            bitmap.allocN32Pixels(width, height);
            surface.readPixels(bitmap, 0, 0);
            byte[] rawBytes = bitmap.readPixels();
            bitmap.close();

            rawPremul = new int[width * height];
            ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(rawPremul);
        }

        // Save stage 0: raw premultiplied as ARGB PNG
        // The int[] is ABGR (0xAARRGGBB). Convert to proper ARGB for PNG.
        savePng(toArgb(rawPremul, width, height), width, height,
                new File(outputDir, "stage0_raw_premultiplied.png"));
        System.out.println("stage0: raw premultiplied (ABGR→ARGB for display)");

        // --- Diagnostic: dump first few pixel values ---
        System.out.println("\n--- Pixel format diagnostic (first 5 non-zero pixels) ---");
        int count = 0;
        for (int i = 0; i < rawPremul.length && count < 5; i++) {
            int px = rawPremul[i];
            int a = (px >>> 24) & 0xFF;
            if (a == 0) continue;
            int r = (px >> 16) & 0xFF;
            int g = (px >> 8) & 0xFF;
            int b = px & 0xFF;
            System.out.printf("  [%d] raw=0x%08X → a=%3d r=%3d g=%3d b=%3d  (ABGR interpretation)%n",
                    i, px, a, r, g, b);
            // Also show what ARGB interpretation would give
            int r2 = (px >> 16) & 0xFF;
            int g2 = (px >> 8) & 0xFF;
            int b2 = px & 0xFF;
            System.out.printf("       ARGB interp → a=%3d r=%3d g=%3d b=%3d  (same for ABGR with A in high byte)%n",
                    a, r2, g2, b2);
            count++;
        }
        System.out.println();

        // --- Stage 1: premultiplied → straight alpha conversion ---
        int[] straightAlpha = rawPremul.clone();
        for (int i = 0; i < straightAlpha.length; i++) {
            int px = straightAlpha[i];
            int a = (px >>> 24);
            if (a > 0 && a < 255) {
                int r = Math.min(255, ((px >> 16) & 0xFF) * 255 / a);
                int g = Math.min(255, ((px >> 8) & 0xFF) * 255 / a);
                int b = Math.min(255, (px & 0xFF) * 255 / a);
                straightAlpha[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        savePng(toArgb(straightAlpha, width, height), width, height,
                new File(outputDir, "stage1_straight_alpha.png"));
        System.out.println("stage1: after premultiplied→straight alpha");

        // Diagnostic: dump edge pixels after conversion
        System.out.println("\n--- After straight alpha conversion (edge pixels) ---");
        count = 0;
        for (int i = 0; i < straightAlpha.length && count < 8; i++) {
            int px = straightAlpha[i];
            int a = (px >>> 24) & 0xFF;
            if (a == 0 || a == 255) continue;
            int r = (px >> 16) & 0xFF;
            int g = (px >> 8) & 0xFF;
            int b = px & 0xFF;
            // Also show what the premultiplied value was
            int premulPx = rawPremul[i];
            int pa = (premulPx >>> 24) & 0xFF;
            int pr = (premulPx >> 16) & 0xFF;
            int pg = (premulPx >> 8) & 0xFF;
            int pb = premulPx & 0xFF;
            System.out.printf("  [%d] premul(a=%3d,r=%3d,g=%3d,b=%3d) → straight(a=%3d,r=%3d,g=%3d,b=%3d)  " +
                            "Δr=%+d Δg=%+d Δb=%+d%n",
                    i, pa, pr, pg, pb, a, r, g, b, r - pr, g - pg, b - pb);
            count++;
        }
        System.out.println();

        // --- Stage 2: normalizeTransparentRgb ---
        int[] normalized = straightAlpha.clone();
        normalizeTransparentRgb(normalized, width, height);
        savePng(toArgb(normalized, width, height), width, height,
                new File(outputDir, "stage2_normalized.png"));
        System.out.println("stage2: after normalizeTransparentRgb");

        // --- Stage 3: 2x scaleNearest ---
        int[] scaled = scaleNearest(normalized, width, height, 2);
        savePng(toArgb(scaled, width * 2, height * 2), width * 2, height * 2,
                new File(outputDir, "stage3_scaled_2x.png"));
        System.out.println("stage3: after 2x nearest-neighbor upscale");

        // --- Stage 4: Wrong format test — interpret ABGR int as ARGB directly ---
        // This simulates what happens if DynamicTexture uses GL_RGBA with the raw int[]
        int[] wrongFormat = rawPremul.clone();
        // Swap R and B channels in each pixel
        for (int i = 0; i < wrongFormat.length; i++) {
            int px = wrongFormat[i];
            int a = (px >>> 24) & 0xFF;
            int r = (px >> 16) & 0xFF;  // actually B in ABGR
            int g = (px >> 8) & 0xFF;
            int b = px & 0xFF;          // actually R in ABGR
            // Swap r↔b to simulate ARGB interpretation of ABGR data
            wrongFormat[i] = (a << 24) | (b << 16) | (g << 8) | r;
        }
        savePng(toArgb(wrongFormat, width, height), width, height,
                new File(outputDir, "stage4_channel_swap_test.png"));
        System.out.println("stage4: channel swap test (R↔B swapped)");

        paragraph.close();
        System.out.println("\nDone! Compare stage5 (Skia ground truth) vs stage0-4.");
        System.out.println("If stage0 matches stage5 → Skia output is correct.");
        System.out.println("If stage1 differs from stage0 → straight alpha conversion loses quality.");
        System.out.println("If stage4 has wrong colors → DynamicTexture format mismatch.");
    }

    // ========== Utility methods (copied from FontPixelUtils for offline use) ==========

    /**
     * Convert ABGR int[] (0xAARRGGBB) to ARGB int[] for BufferedImage.
     */
    private static int[] toArgb(int[] abgr, int w, int h) {
        int[] argb = new int[w * h];
        for (int i = 0; i < abgr.length; i++) {
            int px = abgr[i];
            int a = (px >>> 24) & 0xFF;
            int r = (px >> 16) & 0xFF;
            int g = (px >> 8) & 0xFF;
            int b = px & 0xFF;
            // For white text on black bg, ABGR and ARGB produce the same result
            // because R=G=B for white, and R=G=B=0 for black.
            // But for colored text, we need to check if channels are actually swapped.
            argb[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return argb;
    }

    private static void savePng(int[] argb, int w, int h, File out) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, argb, 0, w);
        ImageIO.write(img, "PNG", out);
    }

    /**
     * Exact copy of FontPixelUtils.normalizeTransparentRgb for offline testing.
     */
    private static void normalizeTransparentRgb(int[] pixels, int width, int height) {
        int[] source = pixels.clone();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if ((source[index] >>> 24) != 0) continue;

                int red = 0, green = 0, blue = 0, count = 0;
                for (int oy = -1; oy <= 1; oy++) {
                    int ny = y + oy;
                    if (ny < 0 || ny >= height) continue;
                    for (int ox = -1; ox <= 1; ox++) {
                        int nx = x + ox;
                        if ((ox == 0 && oy == 0) || nx < 0 || nx >= width) continue;
                        int neighbor = source[ny * width + nx];
                        if ((neighbor >>> 24) == 0) continue;
                        red += (neighbor >>> 16) & 0xFF;
                        green += (neighbor >>> 8) & 0xFF;
                        blue += neighbor & 0xFF;
                        count++;
                    }
                }
                if (count > 0) {
                    pixels[index] = (red / count << 16) | (green / count << 8) | (blue / count);
                } else {
                    pixels[index] = 0x00FFFFFF; // TRANSPARENT_WHITE
                }
            }
        }
    }

    /**
     * Exact copy of FontPixelUtils.scaleNearest for offline testing.
     */
    private static int[] scaleNearest(int[] pixels, int width, int height, int scale) {
        int newW = width * scale;
        int newH = height * scale;
        int[] out = new int[newW * newH];
        for (int y = 0; y < height; y++) {
            int srcRow = y * width;
            for (int sy = 0; sy < scale; sy++) {
                int dstRow = (y * scale + sy) * newW;
                for (int x = 0; x < width; x++) {
                    int value = pixels[srcRow + x];
                    int dst = dstRow + x * scale;
                    for (int sx = 0; sx < scale; sx++) {
                        out[dst + sx] = value;
                    }
                }
            }
        }
        return out;
    }
}
