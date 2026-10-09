package neofontrender.addons.compat;

import net.minecraft.launchwrapper.Launch;
import neofontrender.internal.nightconfig.core.file.CommentedFileConfig;
import neofontrender.internal.nightconfig.toml.TomlFormat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persistent opt-out for the optional YSM body-pose renderer integration. */
public final class YsmPoseConfig {
    private static final String KEY = "compat.ysmBodyPose.enabled";
    private static final String FILE_NAME = "neofontrender-ui-enhancements.toml";
    private static final String COMMENT =
            "Apply flight body poses in the verified YSM player renderer. Restart after changing.";
    private static Boolean cachedEnabled;

    private YsmPoseConfig() {}

    public static synchronized boolean enabled() {
        if (cachedEnabled != null) return cachedEnabled;
        File gameDirectory = Launch.minecraftHome;
        if (gameDirectory == null) {
            throw new IllegalStateException("Cannot read early UI Enhancements config before "
                    + "Launch.minecraftHome is initialized");
        }
        cachedEnabled = readOrCreate(gameDirectory.toPath());
        return cachedEnabled;
    }

    static boolean readOrCreate(Path gameDirectory) {
        Path path = gameDirectory.resolve("config").resolve(FILE_NAME).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) Files.createFile(path);
            try (CommentedFileConfig file = CommentedFileConfig.builder(path, TomlFormat.instance())
                    .sync().preserveInsertionOrder().build()) {
                file.load();
                if (!file.contains(KEY)) {
                    file.set(KEY, true);
                    file.setComment(KEY, COMMENT);
                    file.save();
                }
                Object value = file.get(KEY);
                if (!(value instanceof Boolean enabled)) {
                    throw new IllegalStateException("Invalid UI Enhancements config " + path
                            + " key " + KEY + ": expected a TOML boolean, found "
                            + (value == null ? "null" : value.getClass().getSimpleName()));
                }
                return enabled;
            }
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("Could not read early UI Enhancements config "
                    + path + " key " + KEY, failure);
        }
    }
}
