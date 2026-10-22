package neofontrender.core.font.cosmic;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Runs the native outline SDF and MSDF rasterizers without starting Minecraft.
 * Configure with -DcosmicOutlineIterations=100 and optionally
 * -DcosmicOutlineFont=C:\\path\\font.ttf.
 */
public final class CosmicOutlineBenchmark {
    private static final int RASTER_MAGIC = 0x434F534D;
    private static final String DEFAULT_TEXT = "状态 12345 中 文 / XYZ αβγ";

    private CosmicOutlineBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        CosmicRuntimeSupport.Compatibility compatibility = CosmicRuntimeSupport.ensureLoaded();
        if (!compatibility.isSupported()) {
            throw new IllegalStateException(compatibility.getMessage());
        }

        int iterations = Integer.getInteger("cosmicOutlineIterations", 100);
        float fontSize = Float.parseFloat(System.getProperty("cosmicOutlineFontSize", "16"));
        float rasterScale = Float.parseFloat(System.getProperty("cosmicOutlineScale", "4"));
        int distanceRange = Integer.getInteger("cosmicOutlineRange", 4);
        String text = System.getProperty("cosmicOutlineText", DEFAULT_TEXT);
        String fontPath = System.getProperty("cosmicOutlineFont", "").trim();
        byte[] font = fontPath.isEmpty()
                ? readResource("/assets/neofontrender/fonts/noto_sans_sc-regular.otf")
                : Files.readAllBytes(Path.of(fontPath));

        long engine = CosmicNative.createEngine(
                new byte[][] {font}, new String[] {"benchmark-font"}, "",
                new String[0], "", "", "", "", false, 0,
                fontSize, Locale.getDefault().toLanguageTag());
        if (engine == 0L) {
            throw new IllegalStateException("cosmic-text returned a null engine");
        }
        try {
            System.out.println("Native outline benchmark: family=" + CosmicNative.primaryFamily(engine)
                    + ", text=" + text + ", iterations=" + iterations
                    + ", fontSize=" + fontSize + ", scale=" + rasterScale
                    + ", range=" + distanceRange);
            run(engine, text, iterations, fontSize, rasterScale, distanceRange,
                    "RGBA", (e, value) -> CosmicNative.renderSized(e, value, 0xFFFFFFFF, 0,
                            fontSize, rasterScale));
            runLegacySdf(engine, text, iterations, fontSize, rasterScale, distanceRange);
            run(engine, text, iterations, fontSize, rasterScale, distanceRange,
                    "SDF", (e, value) -> CosmicNative.renderOutlineSdfSized(e, value, 0,
                            fontSize, rasterScale, distanceRange));
            run(engine, text, iterations, fontSize, rasterScale, distanceRange,
                    "MSDF", (e, value) -> CosmicNative.renderMsdfSized(e, value, 0,
                            fontSize, rasterScale, distanceRange));
        } finally {
            CosmicNative.destroyEngine(engine);
        }
    }

    private static void runLegacySdf(long engine, String text, int iterations, float fontSize,
                                     float rasterScale, int distanceRange) {
        int warmup = Math.min(8, Math.max(1, iterations));
        LegacyRaster last = null;
        for (int i = 0; i < warmup; i++) {
            last = legacySdf(CosmicNative.renderSized(engine, text, 0xFFFFFFFF, 0,
                    fontSize, rasterScale), distanceRange);
        }
        long started = System.nanoTime();
        long bytes = 0;
        long checksum = 0;
        for (int i = 0; i < iterations; i++) {
            last = legacySdf(CosmicNative.renderSized(engine, text, 0xFFFFFFFF, 0,
                    fontSize, rasterScale), distanceRange);
            bytes += last.sdf.length;
            checksum = checksum * 31 + checksum(last.sdf);
        }
        long elapsed = System.nanoTime() - started;
        System.out.printf(Locale.ROOT,
                "Legacy SDF: total=%.3f ms, average=%.3f ms, raster=%dx%d, payload=%d, checksum=%016x%n",
                elapsed / 1_000_000.0, elapsed / 1_000_000.0 / Math.max(1, iterations),
                last.width, last.height, bytes / Math.max(1, iterations), checksum);
    }

    private static LegacyRaster legacySdf(byte[] encoded, int distanceRange) {
        if (encoded == null || encoded.length < 36) {
            return new LegacyRaster(0, 0, new byte[0]);
        }
        ByteBuffer data = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        if (data.getInt(0) != RASTER_MAGIC) throw new IllegalStateException("invalid RGBA raster");
        int width = data.getInt(4);
        int height = data.getInt(8);
        int[] pixels = new int[Math.max(0, width * height)];
        data.position(36);
        for (int i = 0; i < pixels.length && data.remaining() >= 4; i++) {
            pixels[i] = data.getInt() & 0xFF00_0000;
        }
        return new LegacyRaster(width, height,
                CosmicSdfGenerator.generate(pixels, width, height, distanceRange));
    }

    private static void run(long engine, String text, int iterations, float fontSize,
                            float rasterScale, int distanceRange, String label,
                            RasterCall call) {
        int warmup = Math.min(8, Math.max(1, iterations));
        byte[] last = null;
        for (int i = 0; i < warmup; i++) {
            last = call.render(engine, text);
        }
        long started = System.nanoTime();
        long bytes = 0;
        long checksum = 0;
        for (int i = 0; i < iterations; i++) {
            last = call.render(engine, text);
            bytes += last == null ? 0 : last.length;
            checksum = checksum * 31 + checksum(last);
        }
        long elapsed = System.nanoTime() - started;
        RasterInfo info = RasterInfo.read(last);
        System.out.printf(Locale.ROOT,
                "%s: total=%.3f ms, average=%.3f ms, raster=%dx%d, payload=%d, checksum=%016x%n",
                label, elapsed / 1_000_000.0, elapsed / 1_000_000.0 / Math.max(1, iterations),
                info.width, info.height, bytes / Math.max(1, iterations), checksum);
    }

    private static long checksum(byte[] bytes) {
        long value = 0;
        if (bytes != null) {
            for (byte item : bytes) value = value * 131 + (item & 0xFF);
        }
        return value;
    }

    private static byte[] readResource(String path) throws Exception {
        try (InputStream input = CosmicOutlineBenchmark.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("missing test font " + path);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] block = new byte[8192];
            int read;
            while ((read = input.read(block)) >= 0) output.write(block, 0, read);
            return output.toByteArray();
        }
    }

    @FunctionalInterface
    private interface RasterCall {
        byte[] render(long engine, String text);
    }

    private record RasterInfo(int width, int height) {
        static RasterInfo read(byte[] bytes) {
            if (bytes == null || bytes.length < 36) return new RasterInfo(0, 0);
            ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            return new RasterInfo(data.getInt(4), data.getInt(8));
        }
    }

    private record LegacyRaster(int width, int height, byte[] sdf) {
    }
}
