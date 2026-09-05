package neofontrender.typst;

import neofontrender.typst.internal.TypstNative;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Locale;

/** Extracts and validates the bundled platform-specific Typst JNI library. */
final class TypstNativeRuntime {
    private static final String RESOURCE_ROOT = "/assets/neofontrender/typst-native/";
    private static boolean attempted;
    private static IOException failure;

    private TypstNativeRuntime() {}

    static synchronized void ensureLoaded() throws IOException {
        if (attempted) {
            if (failure != null) throw failure;
            return;
        }
        attempted = true;
        try {
            Platform platform = detectPlatform();
            if (platform == null) {
                throw new IOException("No bundled Typst native for "
                        + System.getProperty("os.name") + "/" + System.getProperty("os.arch"));
            }
            byte[] library = readResource(RESOURCE_ROOT + platform.directory + "/"
                    + platform.libraryName);
            String hash = sha256(library).substring(0, 16);
            Path directory = Paths.get(System.getProperty("java.io.tmpdir"),
                    "neofontrender", "typst-" + hash);
            Files.createDirectories(directory);
            Path target = directory.resolve(platform.libraryName);
            if (!Files.isRegularFile(target) || Files.size(target) != library.length) {
                Files.write(target, library, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            }
            System.load(target.toAbsolutePath().toString());
            int abi = TypstNative.abiVersion();
            if (abi != TypstNative.ABI_VERSION) {
                throw new IOException("Typst native ABI mismatch: Java="
                        + TypstNative.ABI_VERSION + ", native=" + abi);
            }
        } catch (Throwable error) {
            failure = error instanceof IOException ? (IOException) error
                    : new IOException("Could not load Typst native library", error);
            throw failure;
        }
    }

    private static Platform detectPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        String normalizedArch;
        if (arch.equals("amd64") || arch.equals("x86_64")) normalizedArch = "x86_64";
        else if (arch.equals("aarch64") || arch.equals("arm64")) normalizedArch = "aarch64";
        else return null;
        if (os.contains("win")) {
            return new Platform("windows-" + normalizedArch, "neofontrender_typst.dll");
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return new Platform("macos-" + normalizedArch, "libneofontrender_typst.dylib");
        }
        if (os.contains("linux")) {
            return new Platform("linux-" + normalizedArch + "-gnu", "libneofontrender_typst.so");
        }
        return null;
    }

    private static byte[] readResource(String path) throws IOException {
        try (InputStream input = TypstNativeRuntime.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing Typst native resource " + path);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte current : digest) {
            result.append(String.format(Locale.ROOT, "%02x", current & 0xFF));
        }
        return result.toString();
    }

    private static final class Platform {
        final String directory;
        final String libraryName;

        Platform(String directory, String libraryName) {
            this.directory = directory;
            this.libraryName = libraryName;
        }
    }
}
