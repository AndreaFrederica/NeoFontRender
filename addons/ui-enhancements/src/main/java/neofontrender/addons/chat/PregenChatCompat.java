package neofontrender.addons.chat;

/** Runtime-only optional config bridge. Does not edit or save another mod's configuration. */
public final class PregenChatCompat {
    private PregenChatCompat() {}

    public static boolean optionValue(Object entry) {
        try {
            boolean original = Boolean.TRUE.equals(entry.getClass().getMethod("get").invoke(entry));
            if (!EnhancedChatConfigAccess.tabbedChatEnabled()) return original;
            Class<?> configType = Class.forName("pregenerator.PregenConfig");
            Object config = configType.getField("INSTANCE").get(null);
            Object advancedChatOption = configType.getField("disableAdvChat").get(config);
            return effectiveOption(original, entry == advancedChatOption, true);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not read Chunk Pregenerator chat option", exception);
        }
    }

    static boolean effectiveOption(boolean original, boolean advancedChatOption, boolean uieOwnsChat) {
        return original || (advancedChatOption && uieOwnsChat);
    }
}
