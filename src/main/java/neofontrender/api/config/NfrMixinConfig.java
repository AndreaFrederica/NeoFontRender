package neofontrender.api.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraftforge.fml.common.Loader;
import java.nio.file.*;
import java.util.Locale;

/** Boot-time mixin switches. Values are read before mixin application and require restart. */
public final class NfrMixinConfig {
    private static final Object LOCK = new Object();
    private static CommentedFileConfig file;
    private static void open() {
        synchronized (LOCK) {
            if (file != null) return;
            Path path = Loader.instance().getConfigDir().toPath().resolve("neofontrender-mixins.toml");
            file = CommentedFileConfig.builder(path, TomlFormat.instance()).preserveInsertionOrder().build();
            file.load();
        }
    }
    /** Key is a mixin simple name or fully qualified class name. */
    public static boolean enabled(String mixinClassName) {
        open();
        String simple = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        synchronized (LOCK) {
            Object value = file.getOrElse("mixins." + mixinClassName, null);
            if (value == null) value = file.getOrElse("mixins." + simple, Boolean.TRUE);
            return !(value instanceof Boolean) || (Boolean) value;
        }
    }
    public static Path path() { open(); return Loader.instance().getConfigDir().toPath().resolve("neofontrender-mixins.toml"); }
    private NfrMixinConfig() {}
}
