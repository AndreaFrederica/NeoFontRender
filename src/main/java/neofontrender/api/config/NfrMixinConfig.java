package neofontrender.api.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.nio.file.*;
import java.io.File;
import java.util.Map;

/** Boot-time mixin switches. Values are read before mixin application and require restart. */
public final class NfrMixinConfig {
    private static final Object LOCK = new Object();
    private static CommentedFileConfig file;
    private static void open() {
        synchronized (LOCK) {
            if (file != null) return;
            Path path = configPath();
            try {
                Files.createDirectories(path.getParent());
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot create NeoFontRender config directory: " + path.getParent(), exception);
            }
            file = CommentedFileConfig.builder(path, TomlFormat.instance()).preserveInsertionOrder().build();
            file.load();
        }
    }
    /** Key is a mixin simple name or fully qualified class name. */
    public static boolean enabled(String mixinClassName) {
        open();
        String simple = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        synchronized (LOCK) {
            Object value = valueAt("mixins." + mixinClassName);
            if (value == null) value = valueAt("mixins." + simple);
            if (value == null) value = Boolean.TRUE;
            return !(value instanceof Boolean) || (Boolean) value;
        }
    }

    private static Object valueAt(String key) {
        return file.contains(key) ? file.get(key) : null;
    }
    public static Path path() { open(); return configPath(); }

    /**
     * Mixin plugins run before Forge Loader has initialized its config directory.
     * The launcher-provided home/arguments are checked first; Cleanroom's development
     * launcher deliberately omits those fields and defines its working directory as
     * the instance directory.
     */
    private static Path configPath() {
        try {
            Class<?> launch = Class.forName("net.minecraft.launchwrapper.Launch");
            Object home = launch.getField("minecraftHome").get(null);
            if (home instanceof File) {
                return ((File) home).toPath().resolve("config").resolve("neofontrender-mixins.toml");
            }
            Object blackboard = launch.getField("blackboard").get(null);
            if (blackboard instanceof Map) {
                Map<?, ?> values = (Map<?, ?>) blackboard;
                Path path = pathFromValue(values.get("gameDir"));
                if (path == null) path = pathFromValue(values.get("minecraftHome"));
                if (path == null) {
                    Object launchArgs = values.get("launchArgs");
                    if (launchArgs instanceof Map) {
                        Map<?, ?> args = (Map<?, ?>) launchArgs;
                        path = pathFromValue(args.get("gameDir"));
                    } else if (launchArgs instanceof String[]) {
                        String[] args = (String[]) launchArgs;
                        for (int i = 0; i + 1 < args.length; i++) {
                            if ("--gameDir".equals(args[i])) {
                                path = pathFromValue(args[i + 1]);
                                break;
                            }
                        }
                    }
                }
                if (path != null) {
                    return path.resolve("config").resolve("neofontrender-mixins.toml");
                }
            }
            for (String property : new String[]{"minecraft.gameDir", "fml.gameDir", "gameDir"}) {
                Path path = pathFromValue(System.getProperty(property));
                if (path != null) {
                    return path.resolve("config").resolve("neofontrender-mixins.toml");
                }
            }
            // Cleanroom's development launcher intentionally omits --gameDir and uses
            // the JVM working directory as the configured instance directory.
            Path workingDirectory = pathFromValue(System.getProperty("user.dir"));
            if (workingDirectory != null) {
                return workingDirectory.resolve("config").resolve("neofontrender-mixins.toml");
            }
            throw new IllegalStateException("Minecraft instance directory is unavailable from launcher state");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot resolve Minecraft instance directory before Forge initialization", exception);
        }
    }

    private static Path pathFromValue(Object value) {
        if (value instanceof File) return ((File) value).toPath().toAbsolutePath().normalize();
        if (value instanceof Path) return ((Path) value).toAbsolutePath().normalize();
        if (value != null && !value.toString().trim().isEmpty()) {
            return Paths.get(value.toString()).toAbsolutePath().normalize();
        }
        return null;
    }
    private NfrMixinConfig() {}
}
