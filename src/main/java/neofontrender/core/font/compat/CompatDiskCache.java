package neofontrender.core.font.compat;

import neofontrender.NeoFontRender;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Persistent compat page cache.
 *
 * <p>Layout: {@code <gameDir>/neofontrender/cache/compat-pages/<fingerprint>/page_XX.png}. The
 * fingerprint covers the font chain, size, style, raster scale and page geometry, so changing any
 * of them starts a fresh directory instead of mixing incompatible masks.</p>
 */
final class CompatDiskCache implements AutoCloseable {

    private static final int PAGE_MAGIC_VERSION = 3;

    private final File root;
    private final File directory;
    private final int side;
    private final long maxBytes;
    private final ExecutorService writer;

    CompatDiskCache(File gameDir, String fingerprint, int side, long maxBytes) {
        this.root = new File(gameDir, "neofontrender" + File.separator + "cache" + File.separator + "compat-pages");
        this.directory = new File(root, fingerprint);
        this.side = side;
        this.maxBytes = Math.max(0L, maxBytes);
        this.writer = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "NFR-CompatPageCache");
            thread.setDaemon(true);
            return thread;
        });
        if (!directory.isDirectory() && !directory.mkdirs()) {
            NeoFontRender.LOGGER.warn("Failed to create compat page cache '{}'", directory);
        }
        writer.execute(this::cleanup);
    }

    File directory() {
        return directory;
    }

    byte[] read(int pageIndex) {
        File file = pageFile(pageIndex);
        if (!file.isFile()) {
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null || image.getWidth() != side || image.getHeight() != side) {
                return null;
            }
            byte[] mask = new byte[side * side];
            if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
                image.getRaster().getDataElements(0, 0, side, side, mask);
            } else {
                for (int y = 0; y < side; y++) {
                    for (int x = 0; x < side; x++) {
                        mask[y * side + x] = (byte) (image.getRGB(x, y) & 0xFF);
                    }
                }
            }
            return mask;
        } catch (Throwable error) {
            NeoFontRender.LOGGER.warn("Failed to read compat page {}; regenerating", pageIndex, error);
            return null;
        }
    }

    void write(int pageIndex, byte[] mask) {
        if (mask.length != side * side) {
            return;
        }
        writer.execute(() -> {
            File target = pageFile(pageIndex);
            File temporary = new File(directory, target.getName() + ".tmp");
            try {
                BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_BYTE_GRAY);
                image.getRaster().setDataElements(0, 0, side, side, mask);
                ImageIO.write(image, "png", temporary);
                Path from = temporary.toPath();
                Path to = target.toPath();
                try {
                    Files.move(from, to, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException atomicFailure) {
                    Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Throwable error) {
                NeoFontRender.LOGGER.warn("Failed to write compat page {}", pageIndex, error);
                if (temporary.isFile() && !temporary.delete()) {
                    temporary.deleteOnExit();
                }
            }
            trimToBudget();
        });
    }

    private File pageFile(int pageIndex) {
        return new File(directory, String.format("page_%02x_v%d.png", pageIndex & 0xFF, PAGE_MAGIC_VERSION));
    }

    private void cleanup() {
        try {
            File[] entries = root.listFiles(File::isDirectory);
            if (entries != null && entries.length > 1) {
                List<File> stale = new ArrayList<>();
                for (File entry : entries) {
                    if (!entry.equals(directory)) {
                        stale.add(entry);
                    }
                }
                stale.sort(Comparator.comparingLong(File::lastModified).reversed());
                for (int i = 3; i < stale.size(); i++) {
                    deleteRecursively(stale.get(i));
                }
            }
            trimToBudget();
        } catch (Throwable error) {
            NeoFontRender.LOGGER.debug("Compat page cache cleanup failed", error);
        }
    }

    private void trimToBudget() {
        if (maxBytes <= 0L) {
            return;
        }
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".png"));
        if (files == null || files.length == 0) {
            return;
        }
        long total = 0L;
        for (File file : files) {
            total += file.length();
        }
        if (total <= maxBytes) {
            return;
        }
        List<File> ordered = new ArrayList<>();
        for (File file : files) {
            ordered.add(file);
        }
        ordered.sort(Comparator.comparingLong(File::lastModified));
        for (File file : ordered) {
            if (total <= maxBytes) {
                break;
            }
            long length = file.length();
            if (file.delete()) {
                total -= length;
            }
        }
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        if (!file.delete()) {
            file.deleteOnExit();
        }
    }

    static String fingerprint(String material) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(16);
            for (int index = 0; index < 8; index++) {
                builder.append(String.format("%02x", hash[index] & 0xFF));
            }
            return builder.toString();
        } catch (Throwable error) {
            return Integer.toHexString(material.hashCode());
        }
    }

    @Override
    public void close() {
        writer.shutdown();
    }
}
