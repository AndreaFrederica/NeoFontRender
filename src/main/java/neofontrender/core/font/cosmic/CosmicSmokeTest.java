package neofontrender.core.font.cosmic;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

/** Standalone JNI/shape/raster smoke test used by the Gradle verification task. */
public final class CosmicSmokeTest {
    private CosmicSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        CosmicRuntimeSupport.Compatibility compatibility = CosmicRuntimeSupport.ensureLoaded();
        if (!compatibility.isSupported()) {
            throw new IllegalStateException(compatibility.getMessage());
        }
        byte[] font = readResource("/assets/neofontrender/fonts/sarasa_ui_sc_regular.ttf");
        String primaryFamily = args.length > 0 ? args[0] : "";
        byte[] emojiFont = args.length > 1
                ? Files.readAllBytes(Paths.get(args[1]))
                : readResource("/assets/neofontrender/fonts/noto_color_emoji_regular.ttf");
        long engine = CosmicNative.createEngine(new byte[][] {font, emojiFont}, primaryFamily,
                9.0F, Locale.getDefault().toLanguageTag());
        try {
            String resolvedFamily = CosmicNative.primaryFamily(engine);
            if (!primaryFamily.isEmpty() && !normalizeFamily(resolvedFamily).equals(normalizeFamily(primaryFamily))) {
                throw new IllegalStateException("requested primary family '" + primaryFamily
                        + "' resolved to '" + resolvedFamily + "'");
            }
            String sample = "Cosmic 中文 العربية";
            float width = CosmicNative.measure(engine, sample, 0);
            byte[] raster = CosmicNative.render(engine, sample, 0xFFFFFFFF, 0, 2.0F);
            ByteBuffer data = ByteBuffer.wrap(raster).order(ByteOrder.LITTLE_ENDIAN);
            int magic = data.getInt();
            int pixelWidth = data.getInt();
            int pixelHeight = data.getInt();
            data.getInt(); // offset x
            data.getInt(); // offset y
            float rasterAdvance = data.getFloat();
            float baseline = data.getFloat();
            float scale = data.getFloat();
            if (magic != 0x434F534D || width <= 0.0F || pixelWidth <= 0 || pixelHeight <= 0
                    || rasterAdvance <= 0.0F || baseline <= 0.0F || scale != 2.0F) {
                throw new IllegalStateException("invalid raster: width=" + width + ", pixels=" + pixelWidth + "x" + pixelHeight);
            }
            boolean visible = false;
            while (data.remaining() >= 4) {
                int pixel = data.getInt();
                int alpha = pixel >>> 24;
                if (alpha != 0) {
                    visible = true;
                }
                if (((pixel >>> 16) & 0xFF) > alpha || ((pixel >>> 8) & 0xFF) > alpha || (pixel & 0xFF) > alpha) {
                    throw new IllegalStateException("cosmic-text raster is not premultiplied");
                }
            }
            if (!visible) {
                throw new IllegalStateException("cosmic-text raster contains no visible pixels");
            }
            assertTransparentBorder(raster);
            System.out.println("Cosmic smoke test: family=" + CosmicNative.primaryFamily(engine)
                    + ", advance=" + width + ", baseline=" + baseline + ", raster=" + pixelWidth + "x" + pixelHeight);
            byte[] emojiRaster = CosmicNative.render(engine, "A\uD83D\uDE00\u2764\uFE0F\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66", 0xFFFFFFFF, 0, 2.0F);
            assertTransparentBorder(emojiRaster);
            boolean fallbackChromatic = hasChromaticPixels(emojiRaster);
            long emojiEngine = CosmicNative.createEngine(new byte[][] {emojiFont}, "",
                    9.0F, Locale.getDefault().toLanguageTag());
            boolean primaryChromatic;
            try {
                primaryChromatic = hasChromaticPixels(CosmicNative.render(
                        emojiEngine, "\uD83D\uDE00\u2764\uFE0F", 0xFFFFFFFF, 0, 2.0F));
            } finally {
                CosmicNative.destroyEngine(emojiEngine);
            }
            System.out.println("Cosmic emoji smoke: fallbackColor=" + fallbackChromatic
                    + ", primaryColor=" + primaryChromatic);
            if (!fallbackChromatic || !primaryChromatic) {
                throw new IllegalStateException("cosmic-text emoji color path failed: fallback="
                        + fallbackChromatic + ", primary=" + primaryChromatic);
            }
        } finally {
            CosmicNative.destroyEngine(engine);
        }
    }

    private static String normalizeFamily(String value) {
        return value.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase(Locale.ROOT);
    }

    private static boolean hasChromaticPixels(byte[] raster) {
        ByteBuffer data = ByteBuffer.wrap(raster).order(ByteOrder.LITTLE_ENDIAN);
        data.position(32);
        while (data.remaining() >= 4) {
            int pixel = data.getInt();
            int alpha = pixel >>> 24;
            int red = pixel >>> 16 & 0xFF;
            int green = pixel >>> 8 & 0xFF;
            int blue = pixel & 0xFF;
            if (alpha != 0 && (red != green || green != blue)) {
                return true;
            }
        }
        return false;
    }

    private static void assertTransparentBorder(byte[] raster) {
        ByteBuffer data = ByteBuffer.wrap(raster).order(ByteOrder.LITTLE_ENDIAN);
        data.getInt();
        int width = data.getInt();
        int height = data.getInt();
        if (width <= 0 || height <= 0) {
            return;
        }
        data.position(32);
        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = data.getInt();
        }
        for (int x = 0; x < width; x++) {
            if (pixels[x] != 0 || pixels[(height - 1) * width + x] != 0) {
                throw new IllegalStateException("cosmic-text raster has non-transparent horizontal border");
            }
        }
        for (int y = 0; y < height; y++) {
            if (pixels[y * width] != 0 || pixels[y * width + width - 1] != 0) {
                throw new IllegalStateException("cosmic-text raster has non-transparent vertical border");
            }
        }
    }

    private static byte[] readResource(String path) throws Exception {
        try (InputStream input = CosmicSmokeTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("missing test font " + path);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] block = new byte[8192];
            int read;
            while ((read = input.read(block)) >= 0) {
                output.write(block, 0, read);
            }
            return output.toByteArray();
        }
    }
}
