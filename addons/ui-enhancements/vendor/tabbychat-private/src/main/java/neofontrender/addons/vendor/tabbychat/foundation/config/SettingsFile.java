package neofontrender.addons.vendor.tabbychat.foundation.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Used for creating settings and saving/loading them in the JSON format. Start
 * by creating fields. Mark anything you don't wish to be serialized with {@code transient}.
 * If your setting requires special handling for serialization, override
 *  and use it to customize the {@link Gson}
 * object to your liking.
 *
 * @author Matthew Messinger
 */
public abstract class SettingsFile extends ValueObject {

    private transient final File directory;
    private transient final File file;

    public SettingsFile(String path, String name) {
        this.directory = new File(Minecraft.getMinecraft().mcDataDir, path);
        this.file = new File(directory, name + ".json");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Unable to create settings directory " + directory);
        }
    }

    public File getFile() {
        return file;
    }

    protected File resolve(String path) {
        return new File(directory, path);
    }

    public abstract void loadConfig();

    public abstract void saveConfig();

    protected void saveJson(File file, JsonElement json) {
        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("Unable to create " + parent);
            }
            FileUtils.writeStringToFile(temporary, json.toString(), "UTF-8");
            try {
                Files.move(temporary.toPath(), file.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            TabbyChat.getLogger().error("Unable to save settings " + file, e);
            throw new IllegalStateException("Unable to save settings " + file, e);
        }
    }

    protected JsonElement loadJson(File file) {
        try {
            return new JsonParser().parse(FileUtils.readFileToString(file, "UTF-8"));
        } catch (IOException e) {
            TabbyChat.getLogger().error("Unable to load settings " + file, e);
            throw new IllegalStateException("Unable to load settings " + file, e);
        }
    }

}
