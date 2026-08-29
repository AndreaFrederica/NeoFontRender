package neofontrender.addons.cursor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.ui.NfrUiEnhancements;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Discovers cursor PNGs from .minecraft/neofontrender/cursors and resource packs. */
public enum CursorAssetCatalog implements IResourceManagerReloadListener {
    INSTANCE;

    public static final String RESOURCE_ROOT = "neofontrender/cursors/";
    private final Map<String, CursorAsset> assets = new LinkedHashMap<>();

    @Override
    public synchronized void onResourceManagerReload(IResourceManager resourceManager) {
        reload(resourceManager);
    }

    public synchronized void refresh() {
        reload(Minecraft.getMinecraft().getResourceManager());
    }

    public synchronized List<CursorAsset> assets() {
        return Collections.unmodifiableList(new ArrayList<>(assets.values()));
    }

    public synchronized CursorAsset find(String id) {
        return id == null || id.isEmpty() ? null : assets.get(id);
    }

    public File directory() {
        return new File(Minecraft.getMinecraft().gameDir,
                "neofontrender" + File.separator + "cursors");
    }

    public void openDirectory() {
        File directory = ensureDirectory();
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(directory);
        } catch (Exception error) {
            NfrUiEnhancements.LOGGER.warn("Unable to open cursor directory '{}'", directory, error);
        }
    }

    private void reload(IResourceManager resourceManager) {
        release();
        File localRoot = ensureDirectory();
        Set<String> declared = new HashSet<>();
        scanLocalManifests(localRoot, localRoot, declared);
        scanLocal(localRoot, localRoot, declared);
        scanResourcePacks(resourceManager);
        List<CursorAsset> sorted = new ArrayList<>(assets.values());
        sorted.sort(Comparator.comparing(CursorAsset::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(CursorAsset::id));
        assets.clear();
        for (CursorAsset asset : sorted) assets.put(asset.id(), asset);
        CursorManager.releaseCustomHandles();
    }

    private File ensureDirectory() {
        File directory = directory();
        if (!directory.isDirectory() && !directory.mkdirs()) {
            NfrUiEnhancements.LOGGER.warn("Unable to create cursor directory '{}'", directory);
        }
        return directory;
    }

    private void scanLocal(File root, File current, Set<String> declared) {
        File[] files = current.listFiles();
        if (files == null) return;
        java.util.Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File file : files) {
            if (file.isDirectory()) {
                scanLocal(root, file, declared);
            } else if (file.isFile() && supported(file.getName())) {
                String relative = root.toPath().relativize(file.toPath()).toString().replace('\\', '/');
                if (declared.contains(relative)) continue;
                try (InputStream input = new FileInputStream(file)) {
                    add("local:" + relative, nameFromPath(relative), CursorAsset.Source.LOCAL,
                            readImage(relative, input), 0, 0);
                } catch (Exception error) {
                    NfrUiEnhancements.LOGGER.warn("Unable to load local cursor '{}'", file, error);
                }
            }
        }
    }

    private void scanLocalManifests(File root, File current, Set<String> declared) {
        File manifest = new File(current, "cursors.json");
        if (manifest.isFile()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(manifest), StandardCharsets.UTF_8)) {
                JsonElement json = new JsonParser().parse(reader);
                JsonArray entries = json.isJsonObject() ? json.getAsJsonObject().getAsJsonArray("cursors") : null;
                if (entries != null) {
                    for (JsonElement entry : entries) loadLocalEntry(root, current, entry, declared);
                }
            } catch (Exception error) {
                NfrUiEnhancements.LOGGER.warn("Unable to load local cursor manifest '{}'", manifest, error);
            }
        }
        File[] directories = current.listFiles(File::isDirectory);
        if (directories == null) return;
        java.util.Arrays.sort(directories, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File directory : directories) scanLocalManifests(root, directory, declared);
    }

    private void loadLocalEntry(File root, File manifestDirectory, JsonElement element,
                                Set<String> declared) {
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        if (!object.has("texture")) return;
        File file = new File(manifestDirectory, object.get("texture").getAsString());
        try {
            Path rootPath = root.toPath().toRealPath();
            Path filePath = file.toPath().toRealPath();
            if (!filePath.startsWith(rootPath)) return;
            String relative = rootPath.relativize(filePath)
                    .toString().replace('\\', '/');
            if (!filePath.toFile().isFile() || !supported(filePath.getFileName().toString())) return;
            declared.add(relative);
            String name = object.has("name") ? object.get("name").getAsString() : nameFromPath(relative);
            try (InputStream input = new FileInputStream(filePath.toFile())) {
                add("local:" + relative, name, CursorAsset.Source.LOCAL, readImage(relative, input),
                        integer(object, "hotspotX", 0), integer(object, "hotspotY", 0));
            }
        } catch (Exception error) {
            NfrUiEnhancements.LOGGER.warn("Unable to load local cursor entry '{}'", file, error);
        }
    }

    private void scanResourcePacks(IResourceManager manager) {
        if (manager == null) return;
        for (String domain : manager.getResourceDomains()) {
            ResourceLocation manifest = new ResourceLocation(domain, RESOURCE_ROOT + "cursors.json");
            try (IResource resource = manager.getResource(manifest);
                 Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                JsonElement root = new JsonParser().parse(reader);
                if (!root.isJsonObject()) continue;
                JsonArray entries = root.getAsJsonObject().getAsJsonArray("cursors");
                if (entries == null) continue;
                for (JsonElement entry : entries) loadResourceEntry(manager, domain, entry);
            } catch (java.io.FileNotFoundException ignored) {
                // A namespace without a cursor manifest is expected.
            } catch (Exception error) {
                NfrUiEnhancements.LOGGER.warn("Unable to load cursor manifest '{}'", manifest, error);
            }
        }
    }

    private void loadResourceEntry(IResourceManager manager, String domain, JsonElement element) {
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        if (!object.has("texture")) return;
        String textureValue = object.get("texture").getAsString();
        ResourceLocation texture = textureValue.indexOf(':') >= 0
                ? new ResourceLocation(textureValue)
                : new ResourceLocation(domain, RESOURCE_ROOT + textureValue);
        String name = object.has("name") ? object.get("name").getAsString()
                : nameFromPath(texture.getPath());
        int hotspotX = integer(object, "hotspotX", 0);
        int hotspotY = integer(object, "hotspotY", 0);
        try (IResource resource = manager.getResource(texture);
             InputStream input = resource.getInputStream()) {
            BufferedImage image = readImage(texture.getPath(), input);
            add("resource:" + texture, name, CursorAsset.Source.RESOURCE_PACK,
                    image, hotspotX, hotspotY);
        } catch (Exception error) {
            NfrUiEnhancements.LOGGER.warn("Unable to load resource-pack cursor '{}'", texture, error);
        }
    }

    private void add(String id, String name, CursorAsset.Source source, BufferedImage image,
                     int hotspotX, int hotspotY) {
        try {
            assets.put(id, new CursorAsset(id, name, source, image, hotspotX, hotspotY));
        } catch (RuntimeException error) {
            NfrUiEnhancements.LOGGER.warn("Ignoring invalid cursor image '{}'", id, error);
        }
    }

    private void release() {
        for (CursorAsset asset : assets.values()) asset.releasePreview();
        assets.clear();
    }

    private static int integer(JsonObject object, String key, int fallback) {
        try { return object.has(key) ? object.get(key).getAsInt() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static String nameFromPath(String path) {
        String normalized = path.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static BufferedImage readImage(String name, InputStream input) throws java.io.IOException {
        return name.toLowerCase(Locale.ROOT).endsWith(".svg")
                ? CursorSvgRasterizer.rasterize(input) : ImageIO.read(input);
    }

    private static boolean supported(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".svg");
    }
}
