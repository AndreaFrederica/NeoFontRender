package neofontrender.api.text.paragraph;

import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Registry for optional paragraph engines, separate from glyph render-route selection. */
public final class TextParagraphApi {
    public static final int API_VERSION = 1;
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    private static volatile List<Entry> snapshot = Collections.emptyList();

    private TextParagraphApi() {}

    public static synchronized TextParagraphRegistration register(TextParagraphProvider provider) {
        Objects.requireNonNull(provider, "provider");
        String id = provider.id();
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Paragraph provider id must be namespaced: " + id);
        }
        ENTRIES.removeIf(entry -> entry.id.equals(id));
        Entry entry = new Entry(id, provider);
        ENTRIES.add(entry);
        rebuild();
        return () -> {
            synchronized (TextParagraphApi.class) {
                if (ENTRIES.remove(entry)) rebuild();
            }
        };
    }

    public static TextParagraphProvider.Layout layout(TextParagraphProvider.Request request) {
        if (request == null || Boolean.TRUE.equals(ACTIVE.get())) return null;
        ACTIVE.set(true);
        try {
            for (Entry entry : snapshot) {
                if (!enabled(entry.provider)) continue;
                TextParagraphProvider.Layout result = entry.provider.layout(request);
                if (result != null) return result;
            }
            return null;
        } finally {
            ACTIVE.remove();
        }
    }

    public static List<ITextComponent> splitComponents(
            TextParagraphProvider.ComponentRequest request) {
        if (request == null || Boolean.TRUE.equals(ACTIVE.get())) return null;
        ACTIVE.set(true);
        try {
            for (Entry entry : snapshot) {
                if (!enabled(entry.provider)) continue;
                List<ITextComponent> result = entry.provider.splitComponents(request);
                if (result != null) return result;
            }
            return null;
        } finally {
            ACTIVE.remove();
        }
    }

    public static List<String> providerIds() {
        List<String> result = new ArrayList<>();
        for (Entry entry : snapshot) if (enabled(entry.provider)) result.add(entry.id);
        return Collections.unmodifiableList(result);
    }

    /** Test isolation hook; production code should close its registration handle instead. */
    public static synchronized void clearForTests() {
        ENTRIES.clear();
        rebuild();
    }

    private static boolean enabled(TextParagraphProvider provider) {
        try {
            return provider.isEnabled();
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static synchronized void rebuild() {
        List<Entry> ordered = new ArrayList<>(ENTRIES);
        ordered.sort(Comparator.comparingInt((Entry entry) -> entry.provider.priority()).reversed()
                .thenComparing(entry -> entry.id));
        snapshot = Collections.unmodifiableList(ordered);
    }

    private static final class Entry {
        final String id;
        final TextParagraphProvider provider;

        Entry(String id, TextParagraphProvider provider) {
            this.id = id;
            this.provider = provider;
        }
    }
}
