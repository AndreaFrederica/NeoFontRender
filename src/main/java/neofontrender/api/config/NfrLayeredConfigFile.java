package neofontrender.api.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraftforge.fml.common.Loader;
import java.nio.file.*;
import java.io.IOException;

/** Read-only integration-pack defaults overlaid by a writable user TOML file. */
public final class NfrLayeredConfigFile implements AutoCloseable {
    private final CommentedFileConfig pack, user;
    private final Path userPath;
    public NfrLayeredConfigFile(String ownerId, String fileName) {
        String id = NfrConfigFile.validateId(ownerId);
        String name = NfrConfigFile.validateFileName(fileName);
        userPath = Loader.instance().getConfigDir().toPath().resolve(name);
        try { Files.createDirectories(userPath.getParent()); } catch (IOException e) { throw new IllegalStateException(e); }
        user = CommentedFileConfig.builder(userPath, TomlFormat.instance()).preserveInsertionOrder().build(); user.load();
        Path packPath = Loader.instance().getConfigDir().toPath().resolve("pack-" + id + ".toml");
        pack = CommentedFileConfig.builder(packPath, TomlFormat.instance()).preserveInsertionOrder().build();
        if (Files.exists(packPath)) pack.load();
    }
    public synchronized Object get(String key, Object fallback) {
        if (user.contains(key)) return user.get(key);
        return pack.getOrElse(key, fallback);
    }
    public synchronized boolean containsUser(String key) { return user.contains(key); }
    public synchronized NfrLayeredConfigFile set(String key, Object value) { user.set(key, value); return this; }
    public synchronized void save() { user.save(); }
    public Path userPath() { return userPath; }
    @Override public synchronized void close() { user.close(); pack.close(); }
}
