package neofontrender.addons.typst;

import neofontrender.api.config.NfrConfigApi;
import neofontrender.api.config.NfrConfigFile;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.text.InlineRenderDefaults;

import java.io.File;

/** Independent settings for the optional native Typst engine. */
final class TypstConfig {
    private static NfrConfigFile file;
    private static boolean enabled;
    private static volatile boolean downloadHud = true;
    private static int maxToken = 4096;

    private TypstConfig() {}

    static synchronized void load() {
        if (file == null) {
            file = NfrConfigApi.builder(TypstRendererMod.MOD_ID)
                    .storage(neofontrender.api.config.NfrConfigStorage.INDEPENDENT)
                    .fileName("neofontrender-typst-renderer.toml").open();
        }
        file.define("enabled", false, "Recognize <typst:...> inline documents.")
                .define("downloadHud", true, "Show Typst package download and rendering status.")
                .define("maxToken", 4096, "Maximum UTF-16 source length accepted by one token.");
        enabled = file.getBoolean("enabled", false);
        downloadHud = file.getBoolean("downloadHud", true);
        maxToken = file.getInt("maxToken", 4096, 128, 32768);
        file.save();
    }

    static boolean enabled() { return enabled; }
    static boolean downloadHud() { return downloadHud; }
    static void setDownloadHud(boolean value) { downloadHud = value; }
    static void setEnabled(boolean value) { enabled = value; }
    /** Typst and LaTeX deliberately share the UIE laboratory raster setting. */
    static float oversample() { return InlineRenderDefaults.rasterOversample(); }
    static int maxToken() { return maxToken; }

    static synchronized void save() {
        if (file == null) load();
        file.set("enabled", enabled);
        file.set("downloadHud", downloadHud);
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
