package neofontrender.lab;

import neofontrender.core.font.cosmic.CosmicNative;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * Minecraft-free benchmark for the pixel formats used by the production renderer.
 * It deliberately does not claim to measure GPU time: it measures the shared native
 * raster, Java conversion, modern-shadow blur, and the bytes that would be uploaded.
 */
final class StandaloneRasterBenchmark {
    private static final int MAGIC = 0x434F534D;
    private static final String DEFAULT_TEXT = "F3 XYZ: 1234.56 / 78.90  状态 αβγ";

    private StandaloneRasterBenchmark() {}

    static void run(int iterations) {
        CosmicRuntime.Result runtime = CosmicRuntime.ensureLoaded();
        if (!runtime.available) throw new IllegalStateException(runtime.message);
        byte[] font = readFont();
        String text = System.getProperty("nfrRasterText", DEFAULT_TEXT);
        float size = Float.parseFloat(System.getProperty("nfrRasterFontSize", "8.5"));
        float scale = Float.parseFloat(System.getProperty("nfrRasterScale", "4.0"));
        long engine = CosmicNative.createEngine(new byte[][] {font},
                new String[] {"benchmark-font"}, "", new String[0],
                "", "", "", "", false, 0, size,
                Locale.getDefault().toLanguageTag());
        if (engine == 0L) throw new IllegalStateException("Cosmic returned a null engine");
        try {
            System.out.println("NFR external raster benchmark (modern shadow aware)");
            System.out.println("  text: " + text);
            System.out.println("  iterations: " + iterations + ", fontSize: " + size + ", scale: " + scale);
            byte[] warm = CosmicNative.renderSized(engine, text, 0xFFFFFFFF, 0, size, scale);
            Stats shape = Stats.read(warm);
            runPass(engine, text, iterations, size, scale, "current RGBA + composed modern shadow", false, true, shape);
            runPass(engine, text, iterations, size, scale, "A8 foreground + A8 shadow (split, projected)", true, true, shape);
            runPass(engine, text, iterations, size, scale, "current RGBA without shadow", false, false, shape);
            runFieldPass(engine, text, iterations, size, scale, false);
            runFieldPass(engine, text, iterations, size, scale, true);
            System.out.printf(Locale.ROOT,
                    "  projected upload reduction with shadow: %.2fx (%.1f%% fewer bytes)%n",
                    shape.currentShadowBytes() / (double) Math.max(1L, shape.splitShadowBytes()),
                    100.0 * (1.0 - shape.splitShadowBytes() / (double) Math.max(1L, shape.currentShadowBytes())));
            System.out.println("  note: split mode keeps shadow color in the shader; it adds a draw layer but keeps the same blur work.");
        } finally {
            CosmicNative.destroyEngine(engine);
        }
    }

    private static void runFieldPass(long engine, String text, int iterations,
                                     float size, float scale, boolean msdf) {
        int warmup = Math.min(5, iterations);
        for (int i = 0; i < warmup; i++) field(engine, text, size, scale, msdf);
        long elapsed = 0L, bytes = 0L, checksum = 0L;
        byte[] last = null;
        for (int i = 0; i < iterations; i++) {
            long started = System.nanoTime();
            last = field(engine, text, size, scale, msdf);
            elapsed += System.nanoTime() - started;
            bytes += last.length;
            checksum = checksum * 31 + checksum(last);
        }
        Stats info = Stats.read(last);
        System.out.printf(Locale.ROOT,
                "  %-42s native %.3f ms, payload %.1f KiB, raster=%dx%d, checksum %016x%n",
                msdf ? "compact MSDF RGB8" : "compact outline SDF A8",
                elapsed / 1_000_000.0 / Math.max(1, iterations),
                bytes / (double) Math.max(1, iterations) / 1024.0,
                info.width, info.height, checksum);
    }

    private static byte[] field(long engine, String text, float size, float scale, boolean msdf) {
        return msdf ? CosmicNative.renderMsdfSized(engine, text, 0, size, scale, 4)
                : CosmicNative.renderOutlineSdfSized(engine, text, 0, size, scale, 4);
    }

    private static void runPass(long engine, String text, int iterations, float size, float scale,
                                String label, boolean split, boolean shadow, Stats shape) {
        int warmup = Math.min(10, iterations);
        for (int i = 0; i < warmup; i++) measure(engine, text, size, scale, split, shadow);
        long nativeNanos = 0L, conversionNanos = 0L, shadowNanos = 0L;
        long uploadBytes = 0L;
        long checksum = 0L;
        for (int i = 0; i < iterations; i++) {
            long started = System.nanoTime();
            byte[] encoded = CosmicNative.renderSized(engine, text, 0xFFFFFFFF, 0, size, scale);
            nativeNanos += System.nanoTime() - started;
            started = System.nanoTime();
            Stats stats = Stats.read(encoded);
            int[] rgba = stats.decodeRgba(encoded);
            byte[] alpha = (shadow || split) ? stats.extractAlpha(rgba) : null;
            conversionNanos += System.nanoTime() - started;
            started = System.nanoTime();
            if (shadow) {
                byte[] shadowAlpha = blurShadow(alpha, stats.width, stats.height, 2);
                shadowNanos += System.nanoTime() - started;
                uploadBytes += split ? alpha.length + shadowAlpha.length
                        : 4L * shadowAlpha.length;
                checksum = checksum * 31 + checksum(shadowAlpha);
            } else {
                shadowNanos += System.nanoTime() - started;
                uploadBytes += split ? alpha.length : 4L * stats.width * stats.height;
            }
            if (alpha != null) checksum = checksum * 31 + checksum(alpha);
        }
        double divisor = Math.max(1, iterations) * 1_000_000.0;
        System.out.printf(Locale.ROOT,
                "  %-42s native %.3f ms, convert %.3f ms, shadow %.3f ms, total %.3f ms, upload %.1f KiB, checksum %016x%n",
                label, nativeNanos / divisor, conversionNanos / divisor, shadowNanos / divisor,
                (nativeNanos + conversionNanos + shadowNanos) / divisor,
                uploadBytes / (double) Math.max(1, iterations) / 1024.0, checksum);
    }

    private static void measure(long engine, String text, float size, float scale, boolean split, boolean shadow) {
        byte[] encoded = CosmicNative.renderSized(engine, text, 0xFFFFFFFF, 0, size, scale);
        Stats stats = Stats.read(encoded);
        byte[] alpha = stats.extractAlpha(stats.decodeRgba(encoded));
        if (shadow) blurShadow(alpha, stats.width, stats.height, 2);
    }

    /** Separable box blur used only as a stable external cost model for the production Gaussian path. */
    private static byte[] blurShadow(byte[] source, int width, int height, int radius) {
        if (radius <= 0) return source.clone();
        byte[] horizontal = new byte[source.length];
        byte[] output = new byte[source.length];
        int diameter = radius * 2 + 1;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int sum = 0;
            for (int k = -radius; k <= radius; k++) {
                int sx = Math.max(0, Math.min(width - 1, x + k));
                sum += source[y * width + sx] & 255;
            }
            horizontal[y * width + x] = (byte) (sum / diameter);
        }
        for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) {
            int sum = 0;
            for (int k = -radius; k <= radius; k++) {
                int sy = Math.max(0, Math.min(height - 1, y + k));
                sum += horizontal[sy * width + x] & 255;
            }
            output[y * width + x] = (byte) (sum / diameter);
        }
        return output;
    }

    private static long checksum(byte[] data) {
        long value = 0;
        for (byte item : data) value = value * 131 + (item & 255);
        return value;
    }

    private static byte[] readFont() {
        String configured = System.getProperty("nfrRasterFont", "").trim();
        if (!configured.isEmpty()) {
            try { return Files.readAllBytes(Path.of(configured)); }
            catch (Exception error) { throw new IllegalStateException("cannot read nfrRasterFont", error); }
        }
        try (InputStream input = StandaloneRasterBenchmark.class
                .getResourceAsStream("/assets/neofontrender/fonts/noto_sans_sc-regular.otf")) {
            if (input == null) throw new IllegalStateException("missing bundled benchmark font");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            input.transferTo(output);
            return output.toByteArray();
        } catch (Exception error) {
            throw new IllegalStateException("cannot load benchmark font", error);
        }
    }

    private static final class Stats {
        final int width;
        final int height;
        final int flags;

        private Stats(int width, int height, int flags) {
            this.width = width;
            this.height = height;
            this.flags = flags;
        }

        static Stats read(byte[] payload) {
            if (payload == null || payload.length < 36) throw new IllegalStateException("invalid COSM payload");
            ByteBuffer data = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
            if (data.getInt() != MAGIC) throw new IllegalStateException("invalid COSM magic");
            int width = data.getInt();
            int height = data.getInt();
            data.position(32);
            return new Stats(width, height, data.getInt());
        }

        int[] decodeRgba(byte[] payload) {
            int[] pixels = new int[Math.max(0, width * height)];
            ByteBuffer data = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
            data.position(36);
            for (int i = 0; i < pixels.length; i++) pixels[i] = data.getInt();
            return pixels;
        }

        byte[] extractAlpha(int[] rgba) {
            byte[] alpha = new byte[rgba.length];
            for (int i = 0; i < rgba.length; i++) alpha[i] = (byte) (rgba[i] >>> 24);
            return alpha;
        }

        long currentShadowBytes() {
            // The current non-SDF modern-shadow path composes the glyph into the shadow
            // result and uploads that one RGBA texture, rather than two separate textures.
            return (long) width * height * 4L;
        }

        long splitShadowBytes() {
            // blurShadow() uses the same bounds in both layers in this external cost model.
            return (long) width * height * 2L;
        }
    }
}
