package neofontrender.addons.chat;

import java.util.Map;

/** Persistence boundary for per-scope chat history snapshots. */
interface ChatHistoryStorage {
    /**
     * Loads every valid scope snapshot, returning an empty map after a logged I/O or format
     * failure. Keys are {@link ChatHistoryScope} identifiers.
     */
    Map<String, ChatHistoryData> load();

    /** Atomically writes all scope snapshots and reports whether the write succeeded. */
    boolean save(Map<String, ChatHistoryData> data);
}
