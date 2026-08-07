package neofontrender.addons.flight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.api.flight.FlightApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loads built-in and user-authored flight HUD JSON files, with inheritance and hot reload. */
enum FlightHudThemeManager {
    INSTANCE;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String RESOURCE_ROOT =
            "/assets/neofontrender_ui_enhancements/flight_hud/";
    private static final String[] BUILT_INS = {
            "airbus-a319.json", "airbus-a350.json", "boeing-737.json",
            "airbus-a319-360.json", "airbus-a350-360.json", "boeing-737-360.json",
            "msfs-external.json", "fpv-racing.json", "fpv-freestyle.json",
            "fpv-long-range.json", "stereotype-tactical.json"
    };
    private static final long CHECK_INTERVAL_NANOS = 1_000_000_000L;

    private final Map<String, FlightHudTheme> themes = new LinkedHashMap<>();
    private Path directory;
    private long fingerprint = Long.MIN_VALUE;
    private long lastCheckNanos;

    synchronized void initialize() {
        if (directory == null) {
            directory = Minecraft.getMinecraft().mcDataDir.toPath()
                    .resolve("neofontrender").resolve("flight_hud_themes");
            try {
                Files.createDirectories(directory);
                installTemplate("example-airliner-hud.json");
                installTemplate("README.txt");
            } catch (IOException error) {
                NfrUiEnhancements.LOGGER.warn("Could not prepare flight HUD theme directory {}",
                        directory, error);
            }
        }
        reloadNow();
    }

    synchronized FlightHudTheme current() {
        reloadIfChanged();
        FlightHudTheme selected = themes.get(FlightRollConfig.hudTheme);
        if (selected == null) selected = themes.get("airbus-a350");
        if (selected == null && !themes.isEmpty()) selected = themes.values().iterator().next();
        if (selected == null) {
            selected = new FlightHudTheme();
            selected.validate("fallback");
        }
        return selected;
    }

    synchronized List<String> themeIds() {
        reloadIfChanged();
        return Collections.unmodifiableList(new ArrayList<>(themes.keySet()));
    }

    synchronized Path themeDirectory() {
        if (directory == null) initialize();
        return directory;
    }

    synchronized String displayName(String id) {
        FlightHudTheme theme = themes.get(id);
        return theme == null ? id : theme.name;
    }

    synchronized void reloadNow() {
        if (directory == null) return;
        Map<String, JsonObject> raw = new LinkedHashMap<>();
        for (String file : BUILT_INS) {
            JsonObject object = readResource(RESOURCE_ROOT + file);
            if (object != null) raw.put(id(object, file), object);
        }
        for (Map.Entry<String, String> registered : FlightApi.registeredHudThemes().entrySet()) {
            try (Reader reader = new StringReader(registered.getValue())) {
                JsonObject object = read(reader);
                if (object != null) {
                    object.addProperty("id", registered.getKey());
                    raw.put(registered.getKey(), object);
                }
            } catch (Exception error) {
                NfrUiEnhancements.LOGGER.warn("Invalid registered flight HUD theme {}: {}",
                        registered.getKey(), error.getMessage());
            }
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.json")) {
            for (Path file : files) {
                JsonObject object = readFile(file);
                if (object != null) raw.put(id(object, file.getFileName().toString()), object);
            }
        } catch (IOException error) {
            NfrUiEnhancements.LOGGER.warn("Could not scan flight HUD themes in {}", directory, error);
        }

        Map<String, FlightHudTheme> loaded = new LinkedHashMap<>();
        for (String id : raw.keySet()) {
            JsonObject resolved = resolve(id, raw, new LinkedHashSet<>());
            if (resolved == null) continue;
            try {
                FlightHudTheme theme = GSON.fromJson(resolved, FlightHudTheme.class);
                theme.validate(id);
                loaded.put(theme.id, theme);
            } catch (RuntimeException error) {
                NfrUiEnhancements.LOGGER.warn("Invalid flight HUD theme {}: {}", id, error.getMessage());
            }
        }
        themes.clear();
        themes.putAll(loaded);
        fingerprint = fingerprint(directory);
        lastCheckNanos = System.nanoTime();
        NfrUiEnhancements.LOGGER.info("Loaded {} flight HUD themes from {}", themes.size(), directory);
    }

    private void reloadIfChanged() {
        if (directory == null) initialize();
        long now = System.nanoTime();
        if (now - lastCheckNanos < CHECK_INTERVAL_NANOS) return;
        lastCheckNanos = now;
        if (fingerprint(directory) != fingerprint) reloadNow();
    }

    private static JsonObject resolve(String id, Map<String, JsonObject> raw, Set<String> visiting) {
        JsonObject own = raw.get(id);
        if (own == null) return null;
        if (!visiting.add(id)) {
            NfrUiEnhancements.LOGGER.warn("Cyclic flight HUD theme inheritance at {}", id);
            return null;
        }
        JsonObject merged = new JsonObject();
        if (own.has("extends")) {
            String parentId = own.get("extends").getAsString();
            JsonObject parent = resolve(parentId, raw, visiting);
            if (parent == null) {
                NfrUiEnhancements.LOGGER.warn("Flight HUD theme {} has missing or invalid parent {}",
                        id, parentId);
                visiting.remove(id);
                return null;
            }
            merged = (JsonObject) deepCopy(parent);
        }
        merge(merged, own);
        merged.remove("extends");
        visiting.remove(id);
        return merged;
    }

    static void merge(JsonObject target, JsonObject source) {
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            JsonElement existing = target.get(entry.getKey());
            if (existing != null && existing.isJsonObject() && entry.getValue().isJsonObject()) {
                merge(existing.getAsJsonObject(), entry.getValue().getAsJsonObject());
            } else if ("elements".equals(entry.getKey()) && existing != null
                    && existing.isJsonArray() && entry.getValue().isJsonArray()) {
                mergeElements(existing.getAsJsonArray(), entry.getValue().getAsJsonArray());
            } else {
                target.add(entry.getKey(), deepCopy(entry.getValue()));
            }
        }
    }

    /** Child elements with an existing id patch that element; new ids append in child order. */
    private static void mergeElements(JsonArray target, JsonArray source) {
        for (JsonElement child : source) {
            if (!child.isJsonObject() || !child.getAsJsonObject().has("id")) {
                target.add(deepCopy(child));
                continue;
            }
            String id = child.getAsJsonObject().get("id").getAsString();
            JsonObject match = null;
            for (JsonElement candidate : target) {
                if (candidate.isJsonObject() && candidate.getAsJsonObject().has("id")
                        && id.equals(candidate.getAsJsonObject().get("id").getAsString())) {
                    match = candidate.getAsJsonObject();
                    break;
                }
            }
            if (match == null) target.add(deepCopy(child));
            else merge(match, child.getAsJsonObject());
        }
    }

    private static JsonElement deepCopy(JsonElement element) {
        if (element == null || element.isJsonNull()) return JsonNull.INSTANCE;
        if (element.isJsonPrimitive()) return element.getAsJsonPrimitive();
        if (element.isJsonArray()) {
            JsonArray copy = new JsonArray();
            for (JsonElement child : element.getAsJsonArray()) copy.add(deepCopy(child));
            return copy;
        }
        if (element.isJsonObject()) {
            JsonObject copy = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                copy.add(entry.getKey(), deepCopy(entry.getValue()));
            }
            return copy;
        }
        return new JsonPrimitive(element.toString());
    }

    private static String id(JsonObject object, String fileName) {
        if (object.has("id")) return object.get("id").getAsString();
        return fileName.replaceFirst("\\.json$", "").toLowerCase();
    }

    private static JsonObject readResource(String name) {
        try (InputStream stream = FlightHudThemeManager.class.getResourceAsStream(name)) {
            if (stream == null) return null;
            return read(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception error) {
            NfrUiEnhancements.LOGGER.warn("Could not load built-in flight HUD theme {}", name, error);
            return null;
        }
    }

    private static JsonObject readFile(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return read(reader);
        } catch (Exception error) {
            NfrUiEnhancements.LOGGER.warn("Could not load flight HUD theme {}: {}", path, error.getMessage());
            return null;
        }
    }

    private static JsonObject read(Reader reader) {
        JsonElement value = new JsonParser().parse(reader);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private void installTemplate(String file) throws IOException {
        Path target = directory.resolve(file);
        if (Files.exists(target)) return;
        try (InputStream stream = FlightHudThemeManager.class.getResourceAsStream(RESOURCE_ROOT + file)) {
            if (stream != null) Files.copy(stream, target);
        }
    }

    private static long fingerprint(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) return 0L;
        long value = 1L;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.json")) {
            List<Path> ordered = new ArrayList<>();
            for (Path path : files) ordered.add(path);
            ordered.sort((left, right) -> left.getFileName().toString()
                    .compareToIgnoreCase(right.getFileName().toString()));
            for (Path path : ordered) {
                value = value * 31L + path.getFileName().toString().hashCode();
                value = value * 31L + Files.getLastModifiedTime(path).toMillis();
                value = value * 31L + Files.size(path);
            }
        } catch (IOException ignored) {
            return -1L;
        }
        return value;
    }
}
