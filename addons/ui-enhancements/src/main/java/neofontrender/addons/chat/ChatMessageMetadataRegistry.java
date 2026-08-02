package neofontrender.addons.chat;

import net.minecraft.util.text.ITextComponent;

import java.util.IdentityHashMap;
import java.util.Map;

public final class ChatMessageMetadataRegistry {
    private static final int CACHE_LIMIT = 8192;
    private static final Map<ITextComponent, ChatMessageMetadata> METADATA = new IdentityHashMap<>();
    private static final Map<ITextComponent, Boolean> TIMESTAMPED = new IdentityHashMap<>();

    private ChatMessageMetadataRegistry() {}

    public static ChatMessageMetadata get(ITextComponent component) {
        return component == null ? null : METADATA.get(component);
    }

    public static void put(ITextComponent component, ChatMessageMetadata metadata) {
        if (component == null || metadata == null) return;
        trimIfNeeded();
        METADATA.put(component, metadata);
    }

    public static void copy(ITextComponent from, ITextComponent to) {
        ChatMessageMetadata metadata = get(from);
        if (metadata != null) put(to, metadata);
        if (from != null && to != null && TIMESTAMPED.containsKey(from)) TIMESTAMPED.put(to, Boolean.TRUE);
    }

    public static boolean isTimestamped(ITextComponent component) {
        return component != null && TIMESTAMPED.containsKey(component);
    }

    public static void markTimestamped(ITextComponent component) {
        if (component == null) return;
        trimIfNeeded();
        TIMESTAMPED.put(component, Boolean.TRUE);
    }

    private static void trimIfNeeded() {
        if (METADATA.size() >= CACHE_LIMIT || TIMESTAMPED.size() >= CACHE_LIMIT) {
            METADATA.clear();
            TIMESTAMPED.clear();
        }
    }
}
