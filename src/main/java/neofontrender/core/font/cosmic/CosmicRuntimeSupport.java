package neofontrender.core.font.cosmic;

import neofontrender.NeoFontRender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Locale;

/** Extracts and validates the bundled cosmic-text JNI library. */
public final class CosmicRuntimeSupport {
    private static final String RESOURCE_ROOT = "/assets/neofontrender/natives/";
    private static boolean attempted;
    private static Compatibility compatibility;

    private CosmicRuntimeSupport() {
    }

    public static synchronized Compatibility ensureLoaded() {
        if (attempted) {
            return compatibility;
        }
        attempted = true;
        Platform platform = detectPlatform();
        if (platform == null) {
            return compatibility = new Compatibility(false,
                    "no bundled native for " + System.getProperty("os.name") + "/" + System.getProperty("os.arch"));
        }

        try {
            byte[] library = readResource(RESOURCE_ROOT + platform.resourceDirectory + "/" + platform.libraryName);
            String hash = sha256(library).substring(0, 16);
            Path directory = Paths.get(System.getProperty("java.io.tmpdir"), "neofontrender", "cosmic-" + hash);
            Files.createDirectories(directory);
            Path nativeLibrary = directory.resolve(platform.libraryName);
            if (!Files.isRegularFile(nativeLibrary) || Files.size(nativeLibrary) != library.length) {
                Files.write(nativeLibrary, library, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            System.load(nativeLibrary.toAbsolutePath().toString());
            int abi = CosmicNative.abiVersion();
            if (abi != CosmicNative.ABI_VERSION) {
                return compatibility = new Compatibility(false,
                        "native ABI mismatch: Java=" + CosmicNative.ABI_VERSION + ", native=" + abi);
            }
            return compatibility = new Compatibility(true,
                    "cosmic-text JNI ABI " + abi + " (" + platform.resourceDirectory + ")");
        } catch (Throwable error) {
            // Native loading can fail because of antivirus locks, a missing VC runtime, or an
            // incompatible JVM. Treat it as a selectable-backend failure so the game can still
            // start through the existing AWT fallback.
            NeoFontRender.LOGGER.error("Failed to load cosmic-text native library", error);
            return compatibility = new Compatibility(false, error.toString());
        }
    }

    private static Platform detectPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        String normalizedArch;
        if (arch.equals("amd64") || arch.equals("x86_64")) {
            normalizedArch = "x86_64";
        } else if (arch.equals("aarch64") || arch.equals("arm64")) {
            normalizedArch = "aarch64";
        } else {
            return null;
        }
        if (os.contains("win") && normalizedArch.equals("x86_64")) {
            return new Platform("windows-x86_64", "neofontrender_cosmic.dll");
        }
        if (os.contains("linux")) {
            return new Platform("linux-" + normalizedArch, "libneofontrender_cosmic.so");
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return new Platform("macos-" + normalizedArch, "libneofontrender_cosmic.dylib");
        }
        return null;
    }

    private static byte[] readResource(String path) throws IOException {
        try (InputStream input = CosmicRuntimeSupport.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("missing native resource " + path);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder value = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            value.append(String.format("%02x", b & 0xFF));
        }
        return value.toString();
    }

    public static final class Compatibility {
        private final boolean supported;
        private final String message;

        private Compatibility(boolean supported, String message) {
            this.supported = supported;
            this.message = message;
        }

        public boolean isSupported() {
            return supported;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final class Platform {
        private final String resourceDirectory;
        private final String libraryName;

        private Platform(String resourceDirectory, String libraryName) {
            this.resourceDirectory = resourceDirectory;
            this.libraryName = libraryName;
        }
    }
}
