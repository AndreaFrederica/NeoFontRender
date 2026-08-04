package neofontrender.addons.inline;

import net.minecraft.client.Minecraft;
import neofontrender.addons.ui.NfrUiEnhancements;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Non-blocking index of the client-owned {@code neofontrender/images} gallery. */
final class LocalImageCatalog {
    static final LocalImageCatalog INSTANCE = new LocalImageCatalog();
    private static final long REFRESH_NANOS = 2_000_000_000L;

    private final ExecutorService scanner = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "NFR Local Image Gallery Scanner");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean scanning = new AtomicBoolean();
    private volatile Map<String, Path> aliases = Collections.emptyMap();
    private volatile long lastScan;

    private LocalImageCatalog() {}

    static Path galleryRoot(Path gameDirectory) {
        return gameDirectory.resolve("neofontrender").resolve("images");
    }

    void initialize() {
        requestRefresh(true);
    }

    @Nullable Path image(String alias) {
        requestRefresh(false);
        return aliases.get(normalizeAlias(alias));
    }

    List<String> suggestions(String prefix, int maximum) {
        requestRefresh(false);
        String normalized = normalizeAlias(prefix);
        List<String> result = new ArrayList<>();
        for (String alias : aliases.keySet()) {
            if (!alias.startsWith(normalized)) continue;
            result.add(":" + alias + ":");
            if (result.size() >= maximum) break;
        }
        return result;
    }

    private void requestRefresh(boolean force) {
        long now = System.nanoTime();
        if (!force && now - lastScan < REFRESH_NANOS) return;
        if (!scanning.compareAndSet(false, true)) return;
        lastScan = now;
        scanner.execute(() -> {
            try {
                aliases = scan(galleryRoot(Minecraft.getMinecraft().gameDir.toPath()));
            } finally {
                scanning.set(false);
            }
        });
    }

    private static Map<String, Path> scan(Path root) {
        LinkedHashMap<String, Path> found = new LinkedHashMap<>();
        try {
            Files.createDirectories(root);
            try (java.util.stream.Stream<Path> paths = Files.walk(root, 8)) {
                paths.filter(Files::isRegularFile).sorted().forEach(path -> {
                    String alias = alias(root, path);
                    if (!alias.isEmpty()) found.putIfAbsent(alias, path.toAbsolutePath().normalize());
                });
            }
        } catch (IOException failure) {
            NfrUiEnhancements.LOGGER.warn("Could not scan local image gallery {}", root, failure);
        }
        return Collections.unmodifiableMap(found);
    }

    private static String alias(Path root, Path path) {
        String file = path.getFileName().toString();
        int dot = file.lastIndexOf('.');
        if (dot <= 0 || !supported(file.substring(dot + 1))) return "";
        Path relative = root.relativize(path);
        String raw = relative.toString().substring(0, relative.toString().length()
                - (file.length() - dot)).replace('\\', '-').replace('/', '-');
        return normalizeAlias(raw);
    }

    private static boolean supported(String extension) {
        String value = extension.toLowerCase(Locale.ROOT);
        return value.equals("png") || value.equals("jpg") || value.equals("jpeg")
                || value.equals("gif") || value.equals("bmp");
    }

    private static String normalizeAlias(String value) {
        if (value == null) return "";
        StringBuilder normalized = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = Character.toLowerCase(value.charAt(index));
            if (Character.isLetterOrDigit(current) || current == '_' || current == '+' || current == '-') {
                normalized.append(current);
            } else if (normalized.length() > 0 && normalized.charAt(normalized.length() - 1) != '-') {
                normalized.append('-');
            }
        }
        while (normalized.length() > 0 && normalized.charAt(normalized.length() - 1) == '-') {
            normalized.setLength(normalized.length() - 1);
        }
        return normalized.toString();
    }
}
