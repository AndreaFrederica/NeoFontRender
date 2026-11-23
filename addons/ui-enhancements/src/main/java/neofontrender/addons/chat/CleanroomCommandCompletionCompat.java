package neofontrender.addons.chat;

/** Resolves ownership between UIE and Cleanroom's 0.6.10+ command suggestion UI. */
public final class CleanroomCommandCompletionCompat {
    private CleanroomCommandCompletionCompat() {}

    /** True when Cleanroom's client suggestion engine is present on the runtime classpath. */
    public static boolean available() {
        ClassLoader loader = CleanroomCommandCompletionCompat.class.getClassLoader();
        return loader.getResource("com/cleanroommc/client/chat/suggestion/SuggestionUpdater.class") != null
                && loader.getResource("com/cleanroommc/client/chat/suggestion/SuggestionList.class") != null;
    }

    // TODO(cleanroom-command-core): Replace UI-level arbitration with a structured integration
    // once Cleanroom ships its proposed command registration/dispatch core.

    public static boolean suppressCleanroomSuggestions(boolean commandBlockMode) {
        return shouldSuppress(commandBlockMode,
                EnhancedChatConfigAccess.tabbedChatEnabled(),
                EnhancedChatConfigAccess.commandCompletionEnabled());
    }

    static boolean shouldSuppress(boolean commandBlockMode, boolean tabbedChatEnabled,
                                  boolean commandCompletionEnabled) {
        return !commandBlockMode && tabbedChatEnabled && commandCompletionEnabled;
    }
}
