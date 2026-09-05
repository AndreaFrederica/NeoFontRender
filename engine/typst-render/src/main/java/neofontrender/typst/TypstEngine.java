package neofontrender.typst;

import neofontrender.typst.internal.TypstNative;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Minecraft-free owner of one persistent Typst compiler and rasterizer. */
public final class TypstEngine implements AutoCloseable {
    private static final int RASTER_MAGIC = 0x54595053;
    private static final int HEADER_BYTES = 12;
    private static final int MAX_DIMENSION = 4096;
    private static final long MAX_PIXELS = 8L * 1024L * 1024L;
    private long handle;

    private TypstEngine(long handle) {
        this.handle = handle;
    }

    public static TypstEngine open(Path libraryDirectory) throws IOException {
        Objects.requireNonNull(libraryDirectory, "libraryDirectory");
        Files.createDirectories(libraryDirectory);
        TypstNativeRuntime.ensureLoaded();
        long handle = TypstNative.createEngine(
                libraryDirectory.toAbsolutePath().normalize().toString());
        if (handle == 0L) throw new IOException("Typst native returned a null engine");
        return new TypstEngine(handle);
    }

    public synchronized TypstRaster render(String source, float scale) {
        if (handle == 0L) throw new IllegalStateException("Typst engine is closed");
        if (source == null) throw new IllegalArgumentException("source must not be null");
        if (!Float.isFinite(scale)) throw new IllegalArgumentException("scale must be finite");
        return decode(TypstNative.render(handle, source, scale));
    }

    private static TypstRaster decode(byte[] encoded) {
        if (encoded == null || encoded.length < HEADER_BYTES) {
            throw new IllegalStateException("Typst native returned a truncated raster");
        }
        ByteBuffer data = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        if (data.getInt() != RASTER_MAGIC) {
            throw new IllegalStateException("Typst native returned an invalid raster header");
        }
        int width = data.getInt();
        int height = data.getInt();
        long pixels = (long) width * height;
        if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION
                || pixels > MAX_PIXELS || pixels > Integer.MAX_VALUE
                || data.remaining() != pixels * 4L) {
            throw new IllegalStateException("Typst native returned invalid dimensions "
                    + width + "x" + height);
        }
        int[] argb = new int[(int) pixels];
        for (int index = 0; index < argb.length; index++) {
            int red = data.get() & 0xFF;
            int green = data.get() & 0xFF;
            int blue = data.get() & 0xFF;
            int alpha = data.get() & 0xFF;
            argb[index] = alpha << 24 | red << 16 | green << 8 | blue;
        }
        return new TypstRaster(width, height, argb);
    }

    @Override
    public synchronized void close() {
        long current = handle;
        handle = 0L;
        if (current != 0L) TypstNative.destroyEngine(current);
    }
}
