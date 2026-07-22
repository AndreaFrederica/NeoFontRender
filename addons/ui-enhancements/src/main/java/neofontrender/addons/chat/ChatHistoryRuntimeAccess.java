package neofontrender.addons.chat;

/** Runtime operations supplied to GuiNewChat by the history Mixin. */
public interface ChatHistoryRuntimeAccess {
    /** Immediately trims all live received, wrapped, and sent lists to the configured limit. */
    void nfrUi$trimHistoryToConfiguredLimit();
}
