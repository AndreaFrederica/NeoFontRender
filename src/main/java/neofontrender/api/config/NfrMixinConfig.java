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
            file = load(configPath());
        }
    }

    static CommentedFileConfig load(Path path) {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create NeoFontRender config directory: " + path.getParent(), exception);
        }
        // Defaults must reach disk even if a later mixin fails during startup.
        CommentedFileConfig config = CommentedFileConfig.builder(path, TomlFormat.instance())
                .preserveInsertionOrder().sync().build();
        try {
            config.load();
            return config;
        } catch (RuntimeException exception) {
            config.close();
            throw exception;
        }
    }
    /** Key is a mixin simple name or fully qualified class name. */
    public static boolean enabled(String mixinClassName) {
        open();
        synchronized (LOCK) {
            return enabled(file, mixinClassName);
        }
    }

    static boolean enabled(CommentedFileConfig config, String mixinClassName) {
        String simple = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        String simpleKey = "mixins." + simple;
        Object value = config.get("mixins." + mixinClassName);
        if (value == null) value = config.get(simpleKey);
        if (value == null) {
            // Populate only switches actually queried by a plugin, including newly added
            // mixins on upgrades. Keep existing short-name and full-name overrides intact.
            value = Boolean.TRUE;
            config.set(simpleKey, value);
            config.setComment(simpleKey, " " + mixinClassName);
            if (config.getComment("mixins") == null) {
                config.setComment("mixins", " Boot-time Mixin switches; restart required.\n"
                        + " true = allow, false = disable. Compatibility checks still apply.\n"
                        + " Defaults are added for mixins checked by participating plugins.\n"
                        + " Fully qualified class names take precedence over simple names.");
            }
            config.save();
        }
        return !(value instanceof Boolean) || (Boolean) value;
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
