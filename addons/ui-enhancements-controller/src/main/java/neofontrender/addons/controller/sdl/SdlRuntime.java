package neofontrender.addons.controller.sdl;

import dev.isxander.sdl.Sdl;
import dev.isxander.sdl.SdlLoader;
import dev.isxander.sdl.SdlVersion;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.ServiceLoader;

import static dev.isxander.sdl.SdlInit.SDL_INIT_EVENTS;
import static dev.isxander.sdl.SdlInit.SDL_INIT_GAMEPAD;
import static dev.isxander.sdl.SdlInit.SDL_INIT_JOYSTICK;

final class SdlRuntime {
    private static final String LIBRARY_PROPERTY = "dev.isxander.sdl.library";

    private SdlRuntime() {}

    static Sdl open(Logger logger) {
        try {
            SdlLoader loader = ServiceLoader.load(SdlLoader.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .filter(candidate -> "ffm".equals(candidate.name()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("SDL FFM backend is unavailable"));
            Sdl sdl = loadNative(loader, logger);
            logVersion(sdl, logger);
            if (!sdl.init().SDL_Init(SDL_INIT_JOYSTICK | SDL_INIT_GAMEPAD | SDL_INIT_EVENTS)) {
                throw new IllegalStateException("SDL initialization failed: "
                        + sdl.error().SDL_GetError());
            }
            return sdl;
        } catch (Throwable error) {
            logger.error("Controller support is disabled because SDL3 could not be loaded. "
                    + "Set -Ddev.isxander.sdl.library=<absolute SDL3 path> to override the "
                    + "bundled native.", error);
            return null;
        }
    }

    private static Sdl loadNative(SdlLoader loader, Logger logger) throws Exception {
        String explicitLibrary = System.getProperty(LIBRARY_PROPERTY, "").trim();
        if (!explicitLibrary.isEmpty()) {
            logger.info("Loading SDL3 from explicit library path {}", explicitLibrary);
            return loader.create();
        }

        String resource = bundledNativeResource(
                System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
        if (resource != null) {
            try {
                Path nativeLibrary = extractBundledNative(resource);
                Sdl sdl = loader.create(nativeLibrary);
                logger.info("Loaded bundled SDL3 native {}", resource);
                return sdl;
            } catch (IOException | RuntimeException | LinkageError error) {
                logger.warn("Unable to load bundled SDL3 native {}; trying the system library",
                        resource, error);
            }
        } else {
            logger.warn("No bundled SDL3 native for {}/{}; trying the system library",
                    System.getProperty("os.name"), System.getProperty("os.arch"));
        }
        return loader.create();
    }

    static String bundledNativeResource(String osName, String osArch) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        String arch = osArch == null ? "" : osArch.toLowerCase(Locale.ROOT);
        String architecture;
        if ("amd64".equals(arch) || "x86_64".equals(arch)) architecture = "x86-64";
        else if ("aarch64".equals(arch) || "arm64".equals(arch)) architecture = "aarch64";
        else return null;

        if (os.startsWith("windows")) return "/win32-" + architecture + "/SDL3.dll";
        if (os.startsWith("linux")) return "/linux-" + architecture + "/libSDL3.so";
        if (os.startsWith("mac") || os.startsWith("darwin")) {
            return "/darwin-" + architecture + "/libSDL3.dylib";
        }
        return null;
    }

    private static Path extractBundledNative(String resource) throws Exception {
        byte[] library = readResource(resource);
        String hash = sha256(library).substring(0, 16);
        String fileName = resource.substring(resource.lastIndexOf('/') + 1);
        Path directory = Paths.get(System.getProperty("java.io.tmpdir"), "neofontrender",
                "uie-controller-sdl3-" + hash);
        Files.createDirectories(directory);
        Path nativeLibrary = directory.resolve(fileName);
        if (!Files.isRegularFile(nativeLibrary) || Files.size(nativeLibrary) != library.length) {
            Files.write(nativeLibrary, library, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }
        return nativeLibrary.toAbsolutePath();
    }

    private static byte[] readResource(String resource) throws IOException {
        try (InputStream input = SdlRuntime.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException("Missing bundled SDL3 native " + resource);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder value = new StringBuilder(digest.length * 2);
        for (byte b : digest) value.append(String.format(Locale.ROOT, "%02x", b & 0xFF));
        return value.toString();
    }

    private static void logVersion(Sdl sdl, Logger logger) {
        SdlVersion.SdlVersionNumber nativeVersion =
                SdlVersion.SdlVersionNumber.fromPacked(sdl.version().SDL_GetVersion());
        SdlVersion.SdlVersionNumber bindingVersion = sdl.version().SDL_GetJavaBindingsVersion();
        logger.info("SDL3 native version {}; Java bindings target {}", nativeVersion, bindingVersion);
        if (!nativeVersion.equals(bindingVersion)) {
            logger.warn("SDL3 native and Java binding versions differ; controller behavior may be unstable");
        }
    }
}
