package neofontrender.addons.tooltips;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.build.UiBuildFeatures;
import neofontrender.api.client.tooltip.NfrTooltipApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Loads preview transforms, effects and sound choices from resource-pack JSON files. */
final class PreviewStyleRegistry implements IResourceManagerReloadListener {
    static final PreviewStyleRegistry INSTANCE = new PreviewStyleRegistry();
    private static final ResourceLocation RESOURCE = new ResourceLocation(
            NfrUiEnhancements.MOD_ID, "tooltip_previews/styles.json");
    private static final String CLASSPATH_RESOURCE =
            "/assets/neofontrender_ui_enhancements/tooltip_previews/styles.json";
    private static final String EXAMPLE_RESOURCE =
            "/assets/neofontrender_ui_enhancements/tooltip_previews/example.json";

    private volatile List<Style> styles = Collections.emptyList();
    private volatile List<String> diagnostics = Collections.emptyList();
    private Path userDirectory;

    private PreviewStyleRegistry() {}

    synchronized void initialize() {
        if (userDirectory == null) {
            userDirectory = Minecraft.getMinecraft().gameDir.toPath()
                    .resolve("neofontrender").resolve("tooltip_preview_styles");
            try {
                Files.createDirectories(userDirectory);
                Path example = userDirectory.resolve("example.json");
                if (!Files.exists(example)) {
                    try (InputStream stream = PreviewStyleRegistry.class.getResourceAsStream(EXAMPLE_RESOURCE)) {
                        if (stream != null) Files.copy(stream, example);
                    }
                }
            } catch (IOException error) {
                NfrUiEnhancements.LOGGER.warn("Could not create tooltip preview style directory {}",
                        userDirectory, error);
            }
        }
        reloadNow(Minecraft.getMinecraft().getResourceManager());
    }

    @Override
    public synchronized void onResourceManagerReload(IResourceManager manager) {
        reloadNow(manager);
    }

    private void reloadNow(IResourceManager manager) {
        List<Definition> definitions = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try {
            for (IResource resource : manager.getAllResources(RESOURCE)) {
                String source;
                try { source = resource.getResourcePackName() + ':' + RESOURCE; }
                catch (RuntimeException ignored) { source = RESOURCE.toString(); }
                try (InputStream stream = resource.getInputStream()) {
                    readDefinitions(new InputStreamReader(stream, StandardCharsets.UTF_8),
                            source, definitions, errors);
                }
            }
        } catch (Exception error) {
            errors.add("Could not load resource-pack styles: " + message(error));
        }
        if (definitions.isEmpty()) {
            try (InputStream stream = PreviewStyleRegistry.class.getResourceAsStream(CLASSPATH_RESOURCE)) {
                if (stream != null) readDefinitions(
                        new InputStreamReader(stream, StandardCharsets.UTF_8),
                        "bundled styles.json", definitions, errors);
            } catch (Exception error) {
                errors.add("Could not load bundled styles: " + message(error));
            }
        }
        if (userDirectory != null) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(userDirectory, "*.json")) {
                List<Path> orderedFiles = new ArrayList<>();
                for (Path file : files) orderedFiles.add(file);
                orderedFiles.sort(Comparator.comparing(
                        path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
                for (Path file : orderedFiles) {
                    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        readDefinitions(reader, file.toString(), definitions, errors);
                    } catch (Exception error) {
                        errors.add(file + ": " + message(error));
                    }
                }
            } catch (IOException error) {
                errors.add("Could not scan " + userDirectory + ": " + message(error));
            }
        }

        List<Style> loaded = resolveStyles(definitions, errors);
        loaded.sort(Comparator.comparingInt((Style style) -> style.priority).reversed());
        styles = Collections.unmodifiableList(loaded);
        diagnostics = Collections.unmodifiableList(new ArrayList<>(errors));
        for (String error : errors) NfrUiEnhancements.LOGGER.warn("Tooltip preview style: {}", error);
        if (UiBuildFeatures.DIAGNOSTIC_LOGS) {
            NfrUiEnhancements.LOGGER.info("Loaded {} tooltip preview styles ({} diagnostics)",
                    loaded.size(), errors.size());
        }
    }

    Style match(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) return null;
        for (Style style : styles) if (style.matches(stack)) return style;
        return null;
    }

    List<String> diagnostics() {
        return diagnostics;
    }

    private static void readDefinitions(Reader reader, String source, List<Definition> output,
                                        List<String> errors) {
        JsonElement root = new JsonParser().parse(reader);
        if (root == null) return;
        JsonArray entries = root.isJsonArray() ? root.getAsJsonArray()
                : root.isJsonObject() && root.getAsJsonObject().has("styles")
                && root.getAsJsonObject().get("styles").isJsonArray()
                ? root.getAsJsonObject().getAsJsonArray("styles") : null;
        if (entries == null) {
            errors.add(source + ": expected a styles array");
            return;
        }
        int index = 0;
        for (JsonElement value : entries) {
            if (!value.isJsonObject()) {
                errors.add(source + " styles[" + index + "]: expected an object");
            } else {
                JsonObject object = value.getAsJsonObject();
                String id = string(object, "id", "").trim();
                if (id.isEmpty()) errors.add(source + " styles[" + index + "]: missing id");
                else output.add(new Definition(id, object.deepCopy(), source));
            }
            index++;
        }
    }

    static List<Style> resolveStyles(List<Definition> definitions, List<String> errors) {
        Map<String, Definition> byId = new LinkedHashMap<>();
        for (Definition definition : definitions) {
            Definition replaced = byId.put(definition.id, definition);
            if (replaced != null) errors.add(definition.source + ": style '" + definition.id
                    + "' overrides the definition from " + replaced.source);
        }
        Map<String, JsonObject> resolved = new LinkedHashMap<>();
        Set<String> invalid = new LinkedHashSet<>();
        List<Style> result = new ArrayList<>();
        for (String id : byId.keySet()) {
            JsonObject object = resolveObject(id, byId, resolved, new LinkedHashSet<>(),
                    invalid, errors);
            if (object == null) continue;
            try {
                Style style = Style.parse(object);
                if (style != null) result.add(style);
            } catch (RuntimeException error) {
                errors.add(byId.get(id).source + ": style '" + id + "': " + message(error));
            }
        }
        return result;
    }

    private static JsonObject resolveObject(String id, Map<String, Definition> definitions,
                                            Map<String, JsonObject> resolved, Set<String> visiting,
                                            Set<String> invalid, List<String> errors) {
        if (invalid.contains(id)) return null;
        JsonObject cached = resolved.get(id);
        if (cached != null) return cached.deepCopy();
        Definition definition = definitions.get(id);
        if (definition == null) return null;
        if (!visiting.add(id)) {
            errors.add(definition.source + ": inheritance cycle at style '" + id + "'");
            invalid.addAll(visiting);
            invalid.add(id);
            return null;
        }
        JsonObject result = new JsonObject();
        String parentId = string(definition.object, "extends", "").trim();
        if (!parentId.isEmpty()) {
            if (!definitions.containsKey(parentId)) {
                errors.add(definition.source + ": style '" + id + "' extends unknown style '"
                        + parentId + "'");
            } else {
                JsonObject parent = resolveObject(parentId, definitions, resolved, visiting,
                        invalid, errors);
                if (parent == null) {
                    visiting.remove(id);
                    invalid.add(id);
                    return null;
                }
                result = parent;
            }
        }
        deepMerge(result, definition.object);
        result.remove("extends");
        visiting.remove(id);
        resolved.put(id, result.deepCopy());
        return result;
    }

    private static void deepMerge(JsonObject target, JsonObject overlay) {
        for (Map.Entry<String, JsonElement> entry : overlay.entrySet()) {
            JsonElement current = target.get(entry.getKey());
            JsonElement incoming = entry.getValue();
            if (current != null && current.isJsonObject() && incoming.isJsonObject()) {
                deepMerge(current.getAsJsonObject(), incoming.getAsJsonObject());
            } else {
                target.add(entry.getKey(), incoming.deepCopy());
            }
        }
    }

    static final class Definition {
        final String id;
        final JsonObject object;
        final String source;

        Definition(String id, JsonObject object, String source) {
            this.id = id;
            this.object = object;
            this.source = source;
        }
    }

    static final class Style {
        final String id;
        final List<String> items;
        final List<String> mods;
        final List<String> rarities;
        final JsonObject nbt;
        final int priority;
        final Float scale;
        final Float pitch;
        final Float roll;
        final Float rotationSpeed;
        final Integer width;
        final Integer height;
        final List<String> effects;
        final SoundSpec sound;
        final String armorModel;
        final String armorMode;

        private Style(String id, List<String> items, List<String> mods, List<String> rarities,
                      JsonObject nbt, int priority, Float scale, Float pitch, Float roll,
                      Float rotationSpeed, Integer width, Integer height, List<String> effects,
                      SoundSpec sound, String armorModel, String armorMode) {
            this.id = id;
            this.items = items;
            this.mods = mods;
            this.rarities = rarities;
            this.nbt = nbt;
            this.priority = priority;
            this.scale = scale;
            this.pitch = pitch;
            this.roll = roll;
            this.rotationSpeed = rotationSpeed;
            this.width = width;
            this.height = height;
            this.effects = effects;
            this.sound = sound;
            this.armorModel = armorModel;
            this.armorMode = armorMode;
        }

        static Style parse(JsonObject object) {
            String id = string(object, "id", "style");
            if (id.trim().isEmpty()) return null;
            String armorModel = optionalString(object, "armorModel");
            String armorMode = optionalString(object, "armorMode");
            validateChoice("armorModel", armorModel, "armor_stand", "player");
            validateChoice("armorMode", armorMode, "single_piece", "full_set");
            return new Style(id, strings(object.get("items")), strings(object.get("mods")),
                    strings(object.get("rarities")), object.has("nbt") && object.get("nbt").isJsonObject()
                            ? object.getAsJsonObject("nbt").deepCopy() : null,
                    integer(object, "priority", 0), optionalNumber(object, "scale"),
                    optionalNumber(object, "pitch"), optionalNumber(object, "roll"),
                    optionalNumber(object, "rotationSpeed"), optionalInteger(object, "width"),
                    optionalInteger(object, "height"), strings(object.get("effects")),
                    SoundSpec.parse(object.get("sound")), armorModel, armorMode);
        }

        private static void validateChoice(String field, String value, String... choices) {
            if (value == null) return;
            for (String choice : choices) {
                if (choice.equalsIgnoreCase(value.trim())) return;
            }
            throw new IllegalArgumentException(field + " has unsupported value '" + value + "'");
        }

        float scale(float fallback) { return scale == null ? fallback : scale; }
        float pitch(float fallback) { return pitch == null ? fallback : pitch; }
        float roll(float fallback) { return roll == null ? fallback : roll; }
        float rotationSpeed(float fallback) { return rotationSpeed == null ? fallback : rotationSpeed; }
        int width(int fallback) { return width == null ? fallback : Math.max(0, width); }
        int height(int fallback) { return height == null ? fallback : Math.max(0, height); }
        NfrTooltipApi.PreviewSound sound() { return sound == null ? null : sound.create(); }
        String armorModel(String fallback) { return armorModel == null ? fallback : armorModel; }
        String armorMode(String fallback) { return armorMode == null ? fallback : armorMode; }

        boolean matches(ItemStack stack) {
            ResourceLocation registryName = stack.getItem().getRegistryName();
            String itemId = registryName == null ? "" : registryName.toString().toLowerCase(Locale.ROOT);
            String namespace = registryName == null ? "" : registryName.getNamespace().toLowerCase(Locale.ROOT);
            if (!items.isEmpty() && !matchesAny(items, itemId)) return false;
            if (!mods.isEmpty() && !matchesAny(mods, namespace)) return false;
            if (!rarities.isEmpty() && !matchesAny(rarities,
                    stack.getRarity() == null ? "" : stack.getRarity().name().toLowerCase(Locale.ROOT))) return false;
            if (nbt != null && !matchesNbt(stack.getTagCompound(), nbt)) return false;
            return !items.isEmpty() || !mods.isEmpty() || !rarities.isEmpty() || nbt != null;
        }

        static boolean matchesNbt(NBTTagCompound actual, JsonObject expected) {
            if (actual == null) return false;
            for (Map.Entry<String, JsonElement> entry : expected.entrySet()) {
                NBTBase tag = path(actual, entry.getKey());
                if (tag == null || !matchesTag(tag, entry.getValue())) return false;
            }
            return true;
        }

        private static NBTBase path(NBTTagCompound root, String path) {
            String[] parts = path.split("\\.");
            NBTBase current = root;
            for (String part : parts) {
                if (!(current instanceof NBTTagCompound)) return null;
                current = ((NBTTagCompound) current).getTag(part);
                if (current == null) return null;
            }
            return current;
        }

        private static boolean matchesTag(NBTBase actual, JsonElement expected) {
            if (expected == null || expected.isJsonNull()) return actual == null;
            if (expected.isJsonObject()) {
                return actual instanceof NBTTagCompound
                        && matchesNbt((NBTTagCompound) actual, expected.getAsJsonObject());
            }
            if (expected.isJsonArray()) {
                if (!(actual instanceof NBTTagList)) return false;
                JsonArray values = expected.getAsJsonArray();
                NBTTagList list = (NBTTagList) actual;
                if (list.tagCount() < values.size()) return false;
                for (int index = 0; index < values.size(); index++) {
                    if (!matchesTag(list.get(index), values.get(index))) return false;
                }
                return true;
            }
            if (!expected.isJsonPrimitive()) return false;
            JsonPrimitive primitive = expected.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return actual instanceof NBTPrimitive
                        && (((NBTPrimitive) actual).getByte() != 0) == primitive.getAsBoolean();
            }
            if (primitive.isNumber()) {
                return actual instanceof NBTPrimitive
                        && Double.compare(((NBTPrimitive) actual).getDouble(), primitive.getAsDouble()) == 0;
            }
            String value = primitive.getAsString();
            if (actual instanceof NBTTagString) return ((NBTTagString) actual).getString().equals(value);
            return actual.toString().replace("\"", "").equals(value);
        }

        private static boolean matchesAny(List<String> values, String actual) {
            for (String value : values) {
                String rule = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
                if (rule.equals(actual) || "*".equals(rule)
                        || (rule.endsWith(":*") && actual.startsWith(rule.substring(0, rule.length() - 1)))) {
                    return true;
                }
            }
            return false;
        }
    }

    static final class SoundSpec {
        final Boolean enabled;
        final String event;
        final Float volume;
        final Float pitch;
        final Integer cooldownMillis;

        private SoundSpec(Boolean enabled, String event, Float volume, Float pitch,
                          Integer cooldownMillis) {
            this.enabled = enabled;
            this.event = event;
            this.volume = volume;
            this.pitch = pitch;
            this.cooldownMillis = cooldownMillis;
        }

        static SoundSpec parse(JsonElement element) {
            if (element == null || element.isJsonNull()) return null;
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
                return new SoundSpec(element.getAsBoolean(), null, null, null, null);
            }
            if (!element.isJsonObject()) return null;
            JsonObject object = element.getAsJsonObject();
            Boolean enabled = object.has("enabled") ? object.get("enabled").getAsBoolean() : Boolean.TRUE;
            String event = object.has("event") ? object.get("event").getAsString() : null;
            return new SoundSpec(enabled, event, optionalNumber(object, "volume"),
                    optionalNumber(object, "pitch"), optionalInteger(object, "cooldownMillis"));
        }

        NfrTooltipApi.PreviewSound create() {
            return new NfrTooltipApi.PreviewSound(enabled == null || enabled,
                    event == null ? TooltipConfig.previewSoundEvent : event,
                    volume == null ? TooltipConfig.previewSoundVolume : volume,
                    pitch == null ? TooltipConfig.previewSoundPitch : pitch,
                    cooldownMillis == null ? TooltipConfig.previewSoundCooldownMillis : cooldownMillis);
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        try { return object.has(key) ? object.get(key).getAsString() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static String optionalString(JsonObject object, String key) {
        if (!object.has(key)) return null;
        try {
            String value = object.get(key).getAsString().trim();
            return value.isEmpty() ? null : value;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        Integer value = optionalInteger(object, key);
        return value == null ? fallback : value;
    }

    private static Integer optionalInteger(JsonObject object, String key) {
        try { return object.has(key) ? object.get(key).getAsInt() : null; }
        catch (RuntimeException ignored) { return null; }
    }

    private static Float optionalNumber(JsonObject object, String key) {
        try { return object.has(key) ? object.get(key).getAsFloat() : null; }
        catch (RuntimeException ignored) { return null; }
    }

    private static List<String> strings(JsonElement element) {
        if (element == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement value : element.getAsJsonArray()) if (value.isJsonPrimitive()) {
                result.add(value.getAsString());
            }
        } else if (element.isJsonPrimitive()) result.add(element.getAsString());
        return Collections.unmodifiableList(result);
    }

    private static String message(Throwable error) {
        String value = error.getMessage();
        return value == null || value.trim().isEmpty() ? error.getClass().getSimpleName() : value;
    }
}
