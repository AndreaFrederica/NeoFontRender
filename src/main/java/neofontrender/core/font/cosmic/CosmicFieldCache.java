package neofontrender.core.font.cosmic;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

/** Bounded native-payload cache. Algorithm directories and versioned keys are shared by tools/rendering. */
final class CosmicFieldCache implements AutoCloseable {
    private static final int MAGIC = 0x4E464646;
    // Version 3 switches outline fields to compact A8/RGB8 payloads. Older files are
    // intentionally ignored so a stale four-byte COSM payload cannot be interpreted as a field.
    private static final int VERSION = 3;
    private static final int MAX_PAYLOAD = 8 * 1024 * 1024;
    private static final int PACK_MAGIC = 0x4E465043; // NFPC
    private static final int PACK_VERSION = 1;
    private static final long PACK_LIMIT = 8L * 1024L * 1024L;
    private static final long MEMORY_LIMIT = 16 * 1024 * 1024;
    // Serializes commits and deletion across isolated renderers, so a clear cannot resurrect entries.
    private static final Object DISK_LOCK = new Object();
    private static long generation;
    private final CosmicGlyphAtlas.CacheMode mode;
    private final Path directory;
    private final ThreadPoolExecutor writer;
    private final Map<String, byte[]> memory = new LinkedHashMap<>(64, .75F, true);
    private final Map<String, PackedRef> packed = new HashMap<>();
    private int packedShard;
    private long packedBytes;
    private long memoryBytes;
    private long seenGeneration;
    private boolean closed;

    CosmicFieldCache(CosmicGlyphAtlas.CacheMode mode, boolean enabled) {
        this(mode, enabled ? CosmicGlyphAtlas.defaultDiskCacheDirectory(mode) : null);
    }

    CosmicFieldCache(CosmicGlyphAtlas.CacheMode mode, Path directory) {
        this.mode = mode;
        this.directory = directory;
        synchronized (DISK_LOCK) {
            seenGeneration = generation;
            loadPackedIndex();
        }
        writer = directory == null ? null : new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(8), r -> {
                    Thread thread = new Thread(r, "NFR-FieldCache-" + mode.directory);
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.DiscardPolicy());
    }

    byte[] read(String text, int flags, float size, float scale, int range, String namespace) {
        String key = key(text, flags, size, scale, range, namespace);
        Path file;
        PackedRef packedRef;
        long revision;
        synchronized (DISK_LOCK) {
            checkGeneration();
            if (closed) return null;
            byte[] cached = memory.get(key);
            if (cached != null) return cached;
            if (directory == null) return null;
            packedRef = packed.get(key);
            file = directory.resolve(key + ".bin");
            revision = generation;
        }
        // Filesystem latency and payload allocation stay outside DISK_LOCK. A clear can advance
        // the generation while this read is in flight; the commit below then discards the result.
        byte[] payload;
        try {
            if (packedRef != null) {
                payload = readPacked(packedRef);
            } else {
                long length = Files.size(file);
                if (length < 48 || length > MAX_PAYLOAD + 12L) return null;
                try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
                    if (input.readInt() != MAGIC || input.readInt() != VERSION) return null;
                    int sizeBytes = input.readInt();
                    if (sizeBytes != length - 12) return null;
                    payload = new byte[sizeBytes];
                    input.readFully(payload);
                    if (!valid(payload)) return null;
                }
            }
            if (payload == null || !valid(payload)) return null;
        } catch (IOException | RuntimeException ignored) { return null; }
        synchronized (DISK_LOCK) {
            checkGeneration();
            if (closed || revision != generation) return null;
            byte[] existing = memory.get(key);
            if (existing != null) return existing;
            remember(key, payload);
            return payload;
        }
    }

    void write(String text, int flags, float size, float scale, int range, String namespace, byte[] payload) {
        if (!valid(payload) || payload.length > MAX_PAYLOAD) return;
        String key = key(text, flags, size, scale, range, namespace);
        byte[] copy = payload.clone();
        synchronized (DISK_LOCK) {
            checkGeneration();
            if (closed) return;
            remember(key, copy);
            long revision = generation;
            if (writer != null) writer.execute(() -> {
                synchronized (DISK_LOCK) {
                    if (revision == generation) commit(key, copy);
                }
            });
        }
    }

    /** Tools can wait for their bounded job to persist; the render thread never calls this. */
    void flush() throws InterruptedException {
        if (writer == null) return;
        CountDownLatch barrier = new CountDownLatch(1);
        // A flush follows writes from a single tool worker; blocking queue insertion also handles
        // a full writer queue without silently losing the completion barrier.
        writer.getQueue().put(barrier::countDown);
        writer.prestartCoreThread();
        if (!barrier.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Cache write timed out");
    }

    private void commit(String key, byte[] bytes) {
        if (directory == null) return;
        try {
            Files.createDirectories(directory);
            int keyBytes = key.getBytes(java.nio.charset.StandardCharsets.US_ASCII).length;
            long recordBytes = 8L + keyBytes + bytes.length;
            if (packedBytes == 0L || packedBytes + recordBytes > PACK_LIMIT) {
                packedShard++;
                packedBytes = 0L;
            }
            Path pack = directory.resolve(String.format(Locale.ROOT, "fields-%04d.pack", packedShard));
            boolean fresh = !Files.exists(pack) || Files.size(pack) == 0L;
            long offset = fresh ? 12L : Files.size(pack);
            Path temporary = null;
            try {
                temporary = Files.createTempFile(directory, "fields-", ".tmp");
                try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                    if (fresh) { out.writeInt(PACK_MAGIC); out.writeInt(PACK_VERSION); out.writeInt(packedShard); }
                    out.writeInt(keyBytes); out.writeInt(bytes.length);
                    out.write(key.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                    out.write(bytes);
                }
                try (OutputStream out = Files.newOutputStream(pack, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    Files.copy(temporary, out);
                }
            } finally {
                if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
            packed.put(key, new PackedRef(pack, offset + 8L + keyBytes, bytes.length));
            packedBytes = offset + recordBytes;
            return;
        } catch (IOException ignored) {
            // Fall back to the old one-file format if a packed write cannot be completed.
        }
        Path temporary = null;
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(key + ".bin");
            temporary = Files.createTempFile(directory, key, ".tmp");
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                out.writeInt(MAGIC); out.writeInt(VERSION); out.writeInt(bytes.length); out.write(bytes);
            }
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException ignored) {
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    private byte[] readPacked(PackedRef ref) {
        try (RandomAccessFile input = new RandomAccessFile(ref.path.toFile(), "r")) {
            input.seek(ref.offset);
            byte[] result = new byte[ref.length];
            input.readFully(result);
            return result;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private void loadPackedIndex() {
        packed.clear(); packedShard = 0; packedBytes = 0L;
        if (directory == null) return;
        try {
            Files.createDirectories(directory);
            try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "fields-*.pack")) {
                for (Path path : files) scanPack(path);
            }
        } catch (IOException ignored) { }
    }

    private void scanPack(Path path) {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            if (input.readInt() != PACK_MAGIC || input.readInt() != PACK_VERSION) return;
            int shard = input.readInt();
            long offset = 12L;
            while (true) {
                int keyLength;
                try { keyLength = input.readInt(); } catch (EOFException end) { break; }
                int length = input.readInt();
                if (keyLength <= 0 || keyLength > 128 || length < 36 || length > MAX_PAYLOAD) break;
                byte[] keyBytes = input.readNBytes(keyLength);
                if (keyBytes.length != keyLength) break;
                input.skipNBytes(length);
                if (input.available() < 0) break;
                packed.put(new String(keyBytes, java.nio.charset.StandardCharsets.US_ASCII),
                        new PackedRef(path, offset + 8L + keyLength, length));
                offset += 8L + keyLength + length;
            }
            long size = Files.size(path);
            if (shard > packedShard || (shard == packedShard && size > packedBytes)) {
                packedShard = shard; packedBytes = size;
            }
        } catch (IOException | RuntimeException ignored) { }
    }

    private record PackedRef(Path path, long offset, int length) { }

    static void clearDisk(Path root) {
        synchronized (DISK_LOCK) {
            generation++;
            CosmicGlyphAtlas.clearDiskCacheRoot(root);
        }
    }

    private void checkGeneration() {
        if (seenGeneration != generation) {
            memory.clear(); memoryBytes = 0; seenGeneration = generation;
        }
    }

    private void remember(String key, byte[] bytes) {
        byte[] old = memory.put(key, bytes);
        memoryBytes += bytes.length - (old == null ? 0 : old.length);
        var entries = memory.entrySet().iterator();
        while (memoryBytes > MEMORY_LIMIT && entries.hasNext()) {
            memoryBytes -= entries.next().getValue().length;
            entries.remove();
        }
    }

    private boolean valid(byte[] bytes) {
        if (bytes == null || bytes.length < 36) return false;
        ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (data.getInt(0) != 0x434F534D) return false;
        int width = data.getInt(4), height = data.getInt(8);
        float scale = data.getFloat(28);
        int model = data.getInt(32);
        int expected = mode == CosmicGlyphAtlas.CacheMode.MSDF
                ? CosmicNative.RASTER_MODEL_OUTLINE_MSDF : CosmicNative.RASTER_MODEL_OUTLINE_SDF;
        int channels = mode == CosmicGlyphAtlas.CacheMode.MSDF ? 3 : 1;
        int payload = mode == CosmicGlyphAtlas.CacheMode.MSDF
                ? CosmicNative.RASTER_MODEL_PAYLOAD_RGB8 : CosmicNative.RASTER_MODEL_PAYLOAD_ALPHA8;
        return width >= 0 && height >= 0 && width <= 8192 && height <= 8192
                && 36L + (long) width * height * channels == bytes.length
                && Float.isFinite(scale) && scale > 0 && (model & expected) != 0
                && (model & payload) != 0;
    }

    private String key(String text, int flags, float size, float scale, int range, String namespace) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Length-prefix strings to prevent delimiter ambiguity.
            for (String value : new String[] {Integer.toString(VERSION), Integer.toString(CosmicNative.ABI_VERSION),
                    mode.directory, namespace == null ? "" : namespace, text, Integer.toString(flags),
                    Integer.toString(Float.floatToIntBits(size)), Integer.toString(Float.floatToIntBits(scale)),
                    Integer.toString(range)}) {
                byte[] data = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(4).putInt(data.length).array()); digest.update(data);
            }
            return "field-" + HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    @Override public void close() {
        synchronized (DISK_LOCK) {
            closed = true; memory.clear(); memoryBytes = 0;
            if (writer != null) writer.shutdown();
        }
    }
}
