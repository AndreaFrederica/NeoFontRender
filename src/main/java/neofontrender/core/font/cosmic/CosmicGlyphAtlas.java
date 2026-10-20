package neofontrender.core.font.cosmic;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import net.minecraft.client.Minecraft;
import neofontrender.core.config.NeofontrenderConfig;

import java.nio.ByteBuffer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * A render-thread glyph atlas for the opt-in Cosmic SDF path.
 *
 * <p>The atlas deliberately stores the already-normalized R8 SDF bytes.  A glyph is
 * rasterized once for a (font size, raster scale, style, distance range) strike and
 * subsequent strings only reference its UV rectangle.</p>
 */
final class CosmicGlyphAtlas implements AutoCloseable {
    static final int SIZE = 2048;
    private static final int BORDER = 2;
    private static final int DISK_MAGIC = 0x4E465247; // NFRG
    // Version 3 invalidates entries baked before the outline SDF baseline-offset fix.
    private static final int DISK_VERSION = 3;
    private static final int MAX_DISK_GLYPH_BYTES = SIZE * SIZE;

    private final long engine;
    private final CacheMode mode;
    private final String cacheNamespace;
    private final Path diskCache;
    private final ExecutorService diskWriter;
    private final Map<Key, Glyph> glyphs = new LinkedHashMap<>(256, 0.75F, true);
    private final List<PendingUpload> pendingUploads = new ArrayList<>();
    private int texture;
    private int cursorX = BORDER;
    private int cursorY = BORDER;
    private int rowHeight;
    private long hits;
    private long misses;
    private long diskHits;

    CosmicGlyphAtlas(long engine, String cacheNamespace) {
        this(engine, cacheNamespace, true, CacheMode.SDF_MASK);
    }

    CosmicGlyphAtlas(long engine, String cacheNamespace, boolean persistentCacheEnabled) {
        this(engine, cacheNamespace, persistentCacheEnabled, CacheMode.SDF_MASK);
    }

    CosmicGlyphAtlas(long engine, String cacheNamespace, boolean persistentCacheEnabled, CacheMode mode) {
        this.engine = engine;
        this.mode = mode == null ? CacheMode.SDF_MASK : mode;
        this.cacheNamespace = cacheNamespace == null ? "unknown" : cacheNamespace;
        Path directory = null;
        if (persistentCacheEnabled) {
            try {
                Minecraft minecraft = Minecraft.getMinecraft();
                if (minecraft != null && minecraft.gameDir != null) {
                    directory = cacheRoot().resolve("cosmic-glyphs").resolve(this.mode.directory);
                    Files.createDirectories(directory);
                }
            } catch (Throwable ignored) {
                // Disk caching is an optimization; a read-only or headless environment keeps memory caching.
            }
        }
        diskCache = directory;
        diskWriter = directory == null ? null : Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "NFR-CosmicGlyphCache");
            thread.setDaemon(true);
            return thread;
        });
    }

    Glyph lookup(String value, int flags, float fontSize, float scale, int distanceRange) {
        Key key = new Key(value, flags, Float.floatToIntBits(fontSize),
                Float.floatToIntBits(scale), distanceRange);
        Glyph cached = glyphs.get(key);
        if (cached != null) {
            hits++;
            return cached;
        }
        misses++;
        GlyphDiskEntry disk = readDisk(key);
        if (disk != null) {
            diskHits++;
            Glyph restored = install(key, disk.pixels, disk.width, disk.height,
                    disk.x, disk.y, disk.advance, disk.scale);
            if (restored != null) return restored;
        }
        // Keep atlas baking on the historical mask-to-SDF path. The native outline field is
        // experimental and currently scans every outline segment for every output pixel.
        byte[] encoded = CosmicNative.renderSized(engine, value, 0xFFFFFFFF, flags, fontSize, scale);
        CosmicRasterPreparation.Raster raster = CosmicRasterPreparation.prepare(encoded,
                new CosmicRasterPreparation.Options(1.0F,
                        (NeofontrenderConfig.fontReferenceBaseline()
                                + NeofontrenderConfig.fontBaselineShift())
                                * (Math.max(1.0F, fontSize)
                                / Math.max(1.0F, NeofontrenderConfig.fontSize())),
                        true, distanceRange,
                        false, 0, null));
        CosmicRasterPreparation.Layer layer = raster.foreground();
        if (layer == null) {
            Glyph empty = Glyph.empty(raster.advance());
            glyphs.put(key, empty);
            return empty;
        }
        if (layer.sdf() == null || layer.width() <= 0 || layer.height() <= 0) {
            return null;
        }
        Glyph glyph = install(key, layer.sdf(), layer.width(), layer.height(), layer.x(), layer.y(),
                raster.advance(), raster.scale());
        if (glyph != null) writeDisk(key, layer.sdf(), layer.width(), layer.height(), layer.x(),
                layer.y(), raster.advance(), raster.scale());
        return glyph;
    }

    /** Upload all glyphs baked during this frame with one texture bind and one state transition. */
    void flushUploads() {
        if (pendingUploads.isEmpty()) return;
        ensureTexture();
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int previousAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        try {
            for (PendingUpload upload : pendingUploads) {
                ByteBuffer buffer = BufferUtils.createByteBuffer(upload.pixels.length);
                buffer.put(upload.pixels).flip();
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, upload.x, upload.y,
                        upload.width, upload.height, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, buffer);
            }
        } finally {
            pendingUploads.clear();
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, previousAlignment);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }

    int texture() {
        ensureTexture();
        return texture;
    }

    int size() { return glyphs.size(); }
    long hits() { return hits; }
    long misses() { return misses; }
    long diskHits() { return diskHits; }

    Path diskCacheDirectory() { return diskCache; }

    CacheMode mode() { return mode; }

    /** Removes persisted glyph entries without changing the active atlas texture. */
    void clearDiskCache() {
        if (diskWriter != null) diskWriter.shutdownNow();
        clearDiskCacheDirectory(diskCache);
    }

    static Path defaultDiskCacheDirectory() {
        return defaultDiskCacheDirectory(CacheMode.SDF_MASK);
    }

    static Path defaultDiskCacheDirectory(CacheMode mode) {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null && minecraft.gameDir != null) {
                return minecraft.gameDir.toPath().resolve("neofontrender").resolve("cache")
                        .resolve("cosmic-glyphs").resolve((mode == null ? CacheMode.SDF_MASK : mode).directory);
            }
        } catch (Throwable ignored) { }
        return null;
    }

    static Path cacheRoot() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft == null || minecraft.gameDir == null ? null
                : minecraft.gameDir.toPath().resolve("neofontrender").resolve("cache");
    }

    static Path defaultDiskCacheRoot() {
        Path root = cacheRoot();
        return root == null ? null : root.resolve("cosmic-glyphs");
    }

    static void clearDiskCacheDirectory(Path directory) {
        if (directory == null) return;
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(path -> {
                String name = path.getFileName().toString();
                return name.endsWith(".bin") || name.endsWith(".pack") || name.endsWith(".tmp");
            }).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    static void clearDiskCacheRoot(Path root) {
        if (root == null) return;
        clearDiskCacheDirectory(root.resolve(CacheMode.SDF_MASK.directory));
        clearDiskCacheDirectory(root.resolve(CacheMode.SDF_OUTLINE.directory));
        clearDiskCacheDirectory(root.resolve(CacheMode.MSDF.directory));
        // Keep the root usable for older installations that still have flat entries.
        clearDiskCacheDirectory(root);
    }

    static DiskStats diskStats(Path directory) {
        if (directory == null) return new DiskStats(0L, 0L);
        long entries = 0L;
        long bytes = 0L;
        try (Stream<Path> files = Files.list(directory)) {
            for (Path path : (Iterable<Path>) files::iterator) {
                String name = path.getFileName().toString();
                if (!name.endsWith(".bin") && !name.endsWith(".pack")) continue;
                try {
                    long size = Files.size(path);
                    bytes += size;
                    if (name.endsWith(".bin")) {
                        entries++;
                    } else {
                        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
                            if (input.readInt() != 0x4E465043 || input.readInt() != 1) continue;
                            input.readInt();
                            while (true) {
                                int keyLength;
                                try { keyLength = input.readInt(); } catch (EOFException end) { break; }
                                int payloadLength = input.readInt();
                                if (keyLength <= 0 || keyLength > 128 || payloadLength < 36 || payloadLength > 8 * 1024 * 1024) break;
                                input.skipNBytes((long) keyLength + payloadLength);
                                entries++;
                            }
                        }
                    }
                } catch (IOException ignored) { }
            }
        } catch (IOException ignored) { }
        return new DiskStats(entries, bytes);
    }

    static final class DiskStats {
        final long entries;
        final long bytes;

        DiskStats(long entries, long bytes) {
            this.entries = entries;
            this.bytes = bytes;
        }
    }

    private void ensureTexture() {
        if (texture != 0) return;
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        texture = GL11.glGenTextures();
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, SIZE, SIZE, 0,
                    GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
        }
    }

    private Glyph install(Key key, byte[] pixels, int width, int height, float logicalX, float logicalY,
                          float advance, float rasterScale) {
        if (pixels == null || width <= 0 || height <= 0 || pixels.length != width * height
                || width + BORDER * 2 > SIZE || height + BORDER * 2 > SIZE) return null;
        if (cursorX + width + BORDER > SIZE) {
            cursorX = BORDER;
            cursorY += rowHeight + BORDER;
            rowHeight = 0;
        }
        if (cursorY + height + BORDER > SIZE) return null;
        int x = cursorX;
        int y = cursorY;
        cursorX += width + BORDER;
        rowHeight = Math.max(rowHeight, height);
        pendingUploads.add(new PendingUpload(pixels.clone(), width, height, x, y));
        Glyph glyph = new Glyph(logicalX, logicalY, width / rasterScale, height / rasterScale,
                advance, (float) (x + 0.5F) / SIZE, (float) (y + 0.5F) / SIZE,
                (float) (x + width - 0.5F) / SIZE, (float) (y + height - 0.5F) / SIZE);
        glyphs.put(key, glyph);
        return glyph;
    }

    private GlyphDiskEntry readDisk(Key key) {
        if (diskCache == null) return null;
        Path file = diskCache.resolve(hash(key) + ".bin");
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (input.readInt() != DISK_MAGIC || input.readInt() != DISK_VERSION) return null;
            int width = input.readInt(), height = input.readInt();
            float x = input.readFloat(), y = input.readFloat(), advance = input.readFloat(), scale = input.readFloat();
            long size = (long) width * height;
            if (width <= 0 || height <= 0 || size <= 0 || size > MAX_DISK_GLYPH_BYTES || !Float.isFinite(scale)
                    || scale <= 0.0F) return null;
            byte[] pixels = new byte[(int) size];
            input.readFully(pixels);
            return new GlyphDiskEntry(pixels, width, height, x, y, advance, scale);
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private void writeDisk(Key key, byte[] pixels, int width, int height, float x, float y,
                           float advance, float scale) {
        if (diskCache == null || diskWriter == null || pixels == null || pixels.length > MAX_DISK_GLYPH_BYTES) return;
        byte[] copy = pixels.clone();
        Path file = diskCache.resolve(hash(key) + ".bin");
        diskWriter.execute(() -> {
            try {
                Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
                try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                    output.writeInt(DISK_MAGIC); output.writeInt(DISK_VERSION);
                    output.writeInt(width); output.writeInt(height);
                    output.writeFloat(x); output.writeFloat(y); output.writeFloat(advance); output.writeFloat(scale);
                    output.write(copy);
                }
                try { Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE); }
                catch (Exception atomicFailure) { Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
            } catch (Exception ignored) { }
        });
    }

    private String hash(Key key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((CosmicNative.ABI_VERSION + "|" + mode.directory + "|"
                    + cacheNamespace + "|" + key)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) result.append(String.format("%02x", value & 0xFF));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    @Override
    public void close() {
        if (texture != 0) {
            GL11.glDeleteTextures(texture);
            texture = 0;
        }
        glyphs.clear();
        pendingUploads.clear();
        if (diskWriter != null) diskWriter.shutdown();
        cursorX = BORDER;
        cursorY = BORDER;
        rowHeight = 0;
    }

    enum CacheMode {
        SDF_MASK("sdf-mask"), SDF_OUTLINE("sdf-outline"), MSDF("msdf");

        final String directory;

        CacheMode(String directory) { this.directory = directory; }
    }

    private record Key(String value, int flags, int fontSize, int scale, int distanceRange) {}
    private record PendingUpload(byte[] pixels, int width, int height, int x, int y) {}
    private record GlyphDiskEntry(byte[] pixels, int width, int height, float x, float y,
                                  float advance, float scale) {}

    static final class Glyph {
        final float x;
        final float y;
        final float width;
        final float height;
        final float advance;
        final float u1;
        final float v1;
        final float u2;
        final float v2;

        Glyph(float x, float y, float width, float height, float advance,
              float u1, float v1, float u2, float v2) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.advance = advance;
            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
        }

        static Glyph empty(float advance) {
            return new Glyph(0, 0, 0, 0, advance, 0, 0, 0, 0);
        }
    }
}
