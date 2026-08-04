package neofontrender.addons.server;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Server-side chat persistence: every public chat line, private message and
 * group message is written to the embedded H2 store as it arrives, tagged with
 * a scope so per-group history can be queried later.
 */
public enum ServerChatHistoryManager {
    INSTANCE;

    private static final Logger LOGGER = LogManager.getLogger("NfrUiEnhancementsServer");
    private static final int TRIM_LIMIT = 16384;
    private static final int TRIM_BATCH = 4096;

    private ServerChatHistoryStore store;
    private ServerGroupConfig groups;
    private final Map<String, Integer> pendingTrim = new HashMap<>();
    private int lastTrimLimit = -1;

    public void initialize(MinecraftServer server) {
        Path config = server.getDataDirectory().toPath().resolve("config");
        try {
            store = new ServerChatHistoryStore(config.resolve("neofontrender-ui-chat-server"));
        } catch (SQLException exception) {
            LOGGER.warn("Could not open server chat history database; persistence disabled", exception);
            store = null;
        }
        groups = new ServerGroupConfig(config.resolve("nfr-group-chat.properties"));
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void shutdown() {
        if (store != null) store.close();
        store = null;
        pendingTrim.clear();
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        record(event.getPlayer().getName(), event.getMessage());
    }

    public void record(String sender, String message) {
        insert("CHAT", ServerChatHistoryStore.SCOPE_GLOBAL, sender, "", message);
    }

    public void recordPrivate(String sender, String target, String message) {
        insert("PRIVATE", ServerChatHistoryStore.SCOPE_PRIVATE, sender, target, message);
    }

    public void recordGroup(String sender, String group, String recipients, String message) {
        insert("GROUP", ServerChatHistoryStore.GROUP_PREFIX + group, sender, recipients, message);
    }

    private void insert(String type, String scope, String sender, String recipients, String message) {
        if (store == null || message == null || message.isEmpty()) return;
        try {
            store.insert(type, scope, sender, recipients, System.currentTimeMillis(), message);
            scheduleTrim(scope);
        } catch (ServerChatHistoryException exception) {
            LOGGER.warn("Could not store chat message", exception);
        }
    }

    private void scheduleTrim(String scope) {
        if (lastTrimLimit != TRIM_LIMIT) {
            pendingTrim.clear();
            lastTrimLimit = TRIM_LIMIT;
        }
        if (pendingTrim.merge(scope, 1, Integer::sum) < TRIM_BATCH) return;
        pendingTrim.remove(scope);
        try {
            store.trim(scope, TRIM_LIMIT);
        } catch (ServerChatHistoryException exception) {
            LOGGER.warn("Could not trim chat history", exception);
        }
    }

    public ServerChatHistoryStore store() {
        return store;
    }

    public ServerGroupConfig groups() {
        return groups;
    }
}
