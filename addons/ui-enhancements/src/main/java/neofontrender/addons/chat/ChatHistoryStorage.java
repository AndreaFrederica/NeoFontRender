package neofontrender.addons.chat;

/** Persistence boundary for chat history snapshots. */
interface ChatHistoryStorage {
    /** Loads the last valid snapshot, returning an empty snapshot after a logged I/O or format failure. */
    ChatHistoryData load();

    /** Atomically writes a complete snapshot and reports whether the write succeeded. */
    boolean save(ChatHistoryData data);
}
