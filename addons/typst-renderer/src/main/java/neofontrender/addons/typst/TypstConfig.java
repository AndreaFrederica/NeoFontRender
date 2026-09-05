package neofontrender.addons.typst;

import neofontrender.api.config.NfrConfigApi;
import neofontrender.api.config.NfrConfigFile;
import neofontrender.core.config.NeofontrenderConfig;

import java.io.File;

/** Independent settings for the optional native Typst engine. */
final class TypstConfig {
    private static NfrConfigFile file;
    private static boolean enabled;
    private static float oversample = 2.0F;
    private static int maxToken = 4096;

    private TypstConfig() {}

    static synchronized void load() {
        if (file == null) {
            file = NfrConfigApi.builder(TypstRendererMod.MOD_ID)
                    .storage(neofontrender.api.config.NfrConfigStorage.INDEPENDENT)
                    .fileName("neofontrender-typst-renderer.toml").open();
        }
        file.define("enabled", false, "Recognize <typst:...> inline documents.")
                .define("oversample", 2.0D, "Typst raster scale in pixels per point (0.25-16).")
                .define("maxToken", 4096, "Maximum UTF-16 source length accepted by one token.");
        enabled = file.getBoolean("enabled", false);
        oversample = (float) file.getDouble("oversample", 2.0D, 0.25D, 16.0D);
        maxToken = file.getInt("maxToken", 4096, 128, 32768);
        file.save();
    }

    static boolean enabled() { return enabled; }
    static void setEnabled(boolean value) { enabled = value; }
    static float oversample() { return oversample; }
    static int maxToken() { return maxToken; }

    static synchronized void save() {
        if (file == null) load();
        file.set("enabled", enabled);
        file.set("oversample", (double) oversample);
        file.set("maxToken", maxToken);
        file.save();
    }

    /** Keeps Typst data beside the existing user-managed font directory. */
    static File libraryDirectory() {
        return libraryDirectory(NeofontrenderConfig.fontDirectory().getParentFile());
    }

    static File libraryDirectory(File neofontrenderDirectory) {
        File parent = neofontrenderDirectory == null
                ? new File("neofontrender") : neofontrenderDirectory;
        File directory = new File(parent, "typst");
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IllegalStateException("Could not create Typst library directory: "
                    + directory);
        }
        File packages = new File(directory, "packages");
        if (!packages.isDirectory() && !packages.mkdirs() && !packages.isDirectory()) {
            throw new IllegalStateException("Could not create Typst package directory: " + packages);
        }
        return directory;
    }
}
