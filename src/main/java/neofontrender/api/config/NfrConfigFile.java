package neofontrender.api.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraftforge.fml.common.Loader;
import neofontrender.core.config.NeofontrenderConfig;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** A typed, comment-preserving TOML store for NFR integrations. */
public final class NfrConfigFile implements AutoCloseable {
    private final String ownerId;
    private final NfrConfigStorage storage;
    private final Path path;
    private final String prefix;
    private CommentedFileConfig independent;

    NfrConfigFile(String ownerId, NfrConfigStorage storage, String fileName) {
        this.ownerId = ownerId;
        this.storage = storage;
        if (storage == NfrConfigStorage.INDEPENDENT) {
            this.path = Loader.instance().getConfigDir().toPath().resolve(validateFileName(fileName));
            this.prefix = "";
            try {
                Files.createDirectories(path.getParent());
                if (!Files.exists(path)) Files.createFile(path);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not create config file " + path, exception);
            }
            this.independent = CommentedFileConfig.builder(path, TomlFormat.instance())
                    .preserveInsertionOrder().build();
            this.independent.load();
        } else {
            this.path = Loader.instance().getConfigDir().toPath().resolve("neofontrender.toml");
            this.prefix = "extensions." + ownerId + ".";
        }
    }

    public String ownerId() { return ownerId; }
    public NfrConfigStorage storage() { return storage; }
    public Path path() { return path; }

    public synchronized boolean getBoolean(String key, boolean defaultValue) {
        Object value = get(key, defaultValue);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    public synchronized int getInt(String key, int defaultValue, int min, int max) {
        Object value = get(key, defaultValue);
        int parsed = value instanceof Number ? ((Number) value).intValue() : defaultValue;
        return Math.max(min, Math.min(max, parsed));
    }

    public synchronized double getDouble(String key, double defaultValue, double min, double max) {
        Object value = get(key, defaultValue);
        double parsed = value instanceof Number ? ((Number) value).doubleValue() : defaultValue;
        if (!Double.isFinite(parsed)) parsed = defaultValue;
        return Math.max(min, Math.min(max, parsed));
    }

    public synchronized String getString(String key, String defaultValue) {
        Object value = get(key, defaultValue);
        return value instanceof String ? (String) value : defaultValue;
    }

    /** Returns an untyped NightConfig-compatible value. Prefer the typed getters when possible. */
    public synchronized Object getValue(String key, Object defaultValue) { return get(key, defaultValue); }

    public synchronized boolean contains(String key) {
        checkKey(key);
        return containsFullKey(fullKey(key));
    }

    public synchronized Object remove(String key) {
        checkKey(key);
        String fullKey = fullKey(key);
        if (storage == NfrConfigStorage.INDEPENDENT) return independent.remove(fullKey);
        return NeofontrenderConfig.removeExtensionValue(fullKey);
    }

    public synchronized List<String> getStringList(String key, List<String> defaultValue) {
        Object value = get(key, defaultValue);
        if (!(value instanceof List)) return immutableCopy(defaultValue);
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) if (item != null) result.add(item.toString());
        return Collections.unmodifiableList(result);
    }

    public synchronized NfrConfigFile set(String key, Object value) {
        checkKey(key);
        if (value == null) throw new IllegalArgumentException("Config value must not be null");
        put(fullKey(key), value instanceof List ? new ArrayList<>((List<?>) value) : value);
        return this;
    }

    /** Adds a default only when the key is absent, and optionally records a TOML comment. */
    public synchronized NfrConfigFile define(String key, Object defaultValue, String comment) {
        checkKey(key);
        String fullKey = fullKey(key);
        if (!containsFullKey(fullKey)) put(fullKey, defaultValue instanceof List
                ? new ArrayList<>((List<?>) defaultValue) : defaultValue);
        if (comment != null && !comment.isEmpty()) setComment(fullKey, comment);
        return this;
    }

    public synchronized void save() {
        if (storage == NfrConfigStorage.INDEPENDENT) independent.save();
        else NeofontrenderConfig.saveExtensionValues();
    }

    @Override public synchronized void close() {
        if (independent != null) {
            independent.close();
            independent = null;
        }
    }

    private Object get(String key, Object defaultValue) {
        checkKey(key);
        String fullKey = fullKey(key);
        if (storage == NfrConfigStorage.INDEPENDENT) return independent.getOrElse(fullKey, defaultValue);
        return NeofontrenderConfig.getExtensionValue(fullKey, defaultValue);
    }

    private boolean containsFullKey(String fullKey) {
        if (storage == NfrConfigStorage.INDEPENDENT) return independent.contains(fullKey);
        return NeofontrenderConfig.hasExtensionValue(fullKey);
    }

    private void put(String fullKey, Object value) {
        if (storage == NfrConfigStorage.INDEPENDENT) independent.set(fullKey, value);
        else NeofontrenderConfig.setExtensionValue(fullKey, value);
    }

    private void setComment(String fullKey, String comment) {
        if (storage == NfrConfigStorage.INDEPENDENT) independent.setComment(fullKey, comment);
        else NeofontrenderConfig.setExtensionComment(fullKey, comment);
    }

    private String fullKey(String key) { return prefix + key; }

    static String validateId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+"))
            throw new IllegalArgumentException("ownerId must match [a-z0-9_.-]+");
        return id.toLowerCase(Locale.ROOT);
    }

    static String validateFileName(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_.-]+\\.toml"))
            throw new IllegalArgumentException("fileName must be a simple .toml file name");
        return name;
    }

    private static void checkKey(String key) {
        if (key == null || key.isEmpty() || key.startsWith(".") || key.endsWith(".") || key.contains(".."))
            throw new IllegalArgumentException("Invalid config key: " + key);
    }

    private static List<String> immutableCopy(List<String> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
