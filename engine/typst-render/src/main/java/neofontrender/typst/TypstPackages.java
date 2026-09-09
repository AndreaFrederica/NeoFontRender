package neofontrender.typst;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Cache inventory and deletion. Call mutations only while the compiler is idle. */
public final class TypstPackages {
    private TypstPackages() {}

    public record Entry(String spec, long bytes) {}

    public static String validate(String value) {
        String spec = value.trim();
        if (!spec.matches("@preview/[a-z0-9]+(?:-[a-z0-9]+)*:[0-9]+\\.[0-9]+\\.[0-9]+")) {
            throw new IllegalArgumentException("Expected @preview/package:1.2.3");
        }
        return spec;
    }

    private static Path packagePath(Path library, String value) throws IOException {
        String spec = validate(value);
        Path root = library.toAbsolutePath().normalize();
        String[] parts = spec.substring(1).split("[/ :]");
        Path path = root.resolve("packages").resolve(parts[0]).resolve(parts[1]).resolve(parts[2]);
        for (Path cursor = path; cursor != null; cursor = cursor.getParent()) {
            if (Files.isSymbolicLink(cursor) || (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)
                    && !cursor.toRealPath().equals(cursor.toRealPath(LinkOption.NOFOLLOW_LINKS)))) {
                throw new IOException("Linked package paths are not supported: " + cursor);
            }
        }
        return path;
    }

    public static List<Entry> list(Path library) throws IOException {
        Path preview = library.resolve("packages/preview");
        if (!Files.exists(preview, LinkOption.NOFOLLOW_LINKS)) return List.of();
        List<Entry> result = new ArrayList<>();
        try (var names = Files.newDirectoryStream(preview)) {
            for (Path name : names) {
                if (!Files.isDirectory(name, LinkOption.NOFOLLOW_LINKS)) continue;
                try (var versions = Files.newDirectoryStream(name)) {
                    for (Path version : versions) {
                        String spec = "@preview/" + name.getFileName() + ":" + version.getFileName();
                        Path path;
                        try { path = packagePath(library, spec); }
                        catch (IllegalArgumentException ignored) { continue; }
                        if (!Files.isRegularFile(path.resolve("typst.toml"), LinkOption.NOFOLLOW_LINKS)) continue;
                        long[] size = {0};
                        Files.walkFileTree(path, new SimpleFileVisitor<>() {
                            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                if (attrs.isRegularFile()) size[0] += attrs.size();
                                return FileVisitResult.CONTINUE;
                            }
                        });
                        result.add(new Entry(spec, size[0]));
                    }
                }
            }
        }
        result.sort(Comparator.comparing(Entry::spec));
        return List.copyOf(result);
    }

    public static void delete(Path library, String spec) throws IOException {
        Path path = packagePath(library, spec);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
