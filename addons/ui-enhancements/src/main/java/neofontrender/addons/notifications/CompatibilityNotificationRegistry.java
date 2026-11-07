package neofontrender.addons.notifications;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.common.Loader;
import neofontrender.addons.ui.NfrUiEnhancements;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loads pack-editable compatibility notices and resolves those active in this launch. */
public final class CompatibilityNotificationRegistry {
    public static final String FILE_NAME = "neofontrender-ui-enhancements-notifications.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> SHOWN = new HashSet<>();
    private static List<CompatibilityNotification> entries = new ArrayList<>();
    private static Path path;

    private CompatibilityNotificationRegistry() {}

    public static synchronized void load() {
        path = Loader.instance().getConfigDir().toPath().resolve(FILE_NAME);
        if (!Files.exists(path)) {
            entries = defaults();
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            FileModel model = GSON.fromJson(reader, FileModel.class);
            entries = sanitize(model == null ? null : model.notifications);
        } catch (IOException | RuntimeException exception) {
            NfrUiEnhancements.LOGGER.error("Failed to load compatibility notifications from {}", path, exception);
            entries = defaults();
        }
    }

    public static synchronized List<CompatibilityNotification> all() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public static synchronized List<CompatibilityNotification> pending() {
        List<CompatibilityNotification> result = new ArrayList<>();
        for (CompatibilityNotification entry : entries) {
            if (entry.enabled && Loader.isModLoaded(entry.modid)
                    && (!entry.showOnce || !SHOWN.contains(entry.resolvedId()))) result.add(entry);
        }
        result.sort(Comparator.comparingInt((CompatibilityNotification value) -> value.priority).reversed());
        return result;
    }

    public static synchronized void markShown(List<CompatibilityNotification> notices) {
        for (CompatibilityNotification notice : notices) if (notice.showOnce) SHOWN.add(notice.resolvedId());
    }

    public static synchronized void save() {
        if (path == null) path = Loader.instance().getConfigDir().toPath().resolve(FILE_NAME);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                FileModel model = new FileModel();
                model.notifications = entries;
                GSON.toJson(model, writer);
            }
        } catch (IOException exception) {
            NfrUiEnhancements.LOGGER.error("Failed to save compatibility notifications to {}", path, exception);
        }
    }

    private static List<CompatibilityNotification> sanitize(List<CompatibilityNotification> values) {
        if (values == null) return new ArrayList<>();
        List<CompatibilityNotification> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (CompatibilityNotification value : values) {
            if (value != null && value.valid() && ids.add(value.resolvedId())) result.add(value);
        }
        return result;
    }

    private static List<CompatibilityNotification> defaults() {
        try (InputStream stream = CompatibilityNotificationRegistry.class.getResourceAsStream(
                "/assets/neofontrender_ui_enhancements/default-compatibility-notifications.json")) {
            if (stream == null) return new ArrayList<>();
            try (Reader reader = new java.io.InputStreamReader(stream, StandardCharsets.UTF_8)) {
                FileModel model = GSON.fromJson(reader, FileModel.class);
                return sanitize(model == null ? null : model.notifications);
            }
        } catch (IOException | RuntimeException exception) {
            NfrUiEnhancements.LOGGER.error("Failed to load bundled compatibility notifications", exception);
            return new ArrayList<>();
        }
    }

    private static final class FileModel {
        List<CompatibilityNotification> notifications = new ArrayList<>();
    }
}
