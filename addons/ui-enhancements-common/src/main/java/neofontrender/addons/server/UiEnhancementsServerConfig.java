package neofontrender.addons.server;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import neofontrender.addons.chat.network.ChatCharacterPolicy;

/** Used by both the integrated server and the dedicated server companion. */
public final class UiEnhancementsServerConfig {
    private UiEnhancementsServerConfig() {}

    public static void load(File directory) {
        Configuration config = new Configuration(new File(directory, "revo-ui-server.cfg"));
        config.load();
        boolean allow = config.getBoolean("allowSectionSign", "chat", false,
                "Allow section signs in received chat and commands. Requires a server restart.");
        ChatCharacterPolicy.configureServer(allow);
        if (config.hasChanged()) config.save();
    }
}
