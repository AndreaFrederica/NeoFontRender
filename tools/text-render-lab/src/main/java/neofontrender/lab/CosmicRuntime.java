package neofontrender.lab;

import neofontrender.core.font.cosmic.CosmicNative;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Locale;

/** Loads the host-specific Cosmic library bundled in the standalone executable JAR. */
final class CosmicRuntime {
    private static Result result;

    private CosmicRuntime() {}

    static synchronized Result ensureLoaded() {
        if (result != null) return result;
        Platform platform = Platform.current();
        if (platform == null) {
            return result = new Result(false, "unsupported platform "
                    + System.getProperty("os.name") + '/' + System.getProperty("os.arch"));
        }
        String resource = "/assets/neofontrender/natives/" + platform.directory
                + '/' + platform.library;
        try {
            byte[] bytes = readResource(resource);
            String hash = sha256(bytes).substring(0, 16);
            Path directory = Path.of(System.getProperty("java.io.tmpdir"),
                    "neofontrender-text-lab", "cosmic-" + hash);
            Files.createDirectories(directory);
            Path library = directory.resolve(platform.library);
            if (!Files.isRegularFile(library) || Files.size(library) != bytes.length) {
                Files.write(library, bytes, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            }
            System.load(library.toAbsolutePath().toString());
            int abi = CosmicNative.abiVersion();
            if (abi != CosmicNative.ABI_VERSION) {
                return result = new Result(false, "native ABI mismatch: Java="
                        + CosmicNative.ABI_VERSION + ", native=" + abi);
            }
            return result = new Result(true, "Cosmic JNI ABI " + abi + " ("
                    + platform.directory + ')');
        } catch (Throwable error) {
            return result = new Result(false, error.toString());
        }
    }

    static byte[] readResource(String path) throws IOException {
        try (InputStream input = CosmicRuntime.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("missing resource " + path);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder value = new StringBuilder(digest.length * 2);
        for (byte part : digest) value.append(String.format("%02x", part & 0xFF));
        return value.toString();
    }

    static final class Result {
        final boolean available;
        final String message;

        Result(boolean available, String message) {
            this.available = available;
            this.message = message;
        }
    }

    private static final class Platform {
        final String directory;
        final String library;

        Platform(String directory, String library) {
            this.directory = directory;
            this.library = library;
        }

        static Platform current() {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
            String normalized;
            if (arch.equals("amd64") || arch.equals("x86_64")) normalized = "x86_64";
            else if (arch.equals("aarch64") || arch.equals("arm64")) normalized = "aarch64";
            else return null;
            if (os.contains("win")) {
                return new Platform("windows-" + normalized, "neofontrender_cosmic.dll");
            }
            if (os.contains("mac") || os.contains("darwin")) {
                return new Platform("macos-" + normalized, "libneofontrender_cosmic.dylib");
            }
            if (os.contains("linux")) {
                return new Platform("linux-" + normalized + "-gnu",
                        "libneofontrender_cosmic.so");
            }
            return null;
        }
    }
}
