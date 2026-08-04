package neofontrender.api.text;

import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Registry for optional CJK paragraph layout engines. */
public final class CjkParagraphLayoutRegistry {
    private static final Logger LOGGER = LogManager.getLogger("NeoFontRender/CJKLayout");
    private static final List<CjkParagraphLayoutProvider> PROVIDERS = new ArrayList<>();
    private static final Set<String> REPORTED_FAILURES = new HashSet<>();
    private static final ThreadLocal<Boolean> DISPATCHING = ThreadLocal.withInitial(() -> false);

    private CjkParagraphLayoutRegistry() {}

    public static synchronized void register(CjkParagraphLayoutProvider provider) {
        if (provider == null || provider.id() == null || provider.id().trim().isEmpty()) {
            throw new IllegalArgumentException("CJK paragraph provider must have a non-empty id");
        }
        unregister(provider.id());
        PROVIDERS.add(provider);
        PROVIDERS.sort(Comparator.comparingInt(CjkParagraphLayoutProvider::priority).reversed()
                .thenComparing(CjkParagraphLayoutProvider::id));
    }

    public static synchronized boolean unregister(String id) {
        return id != null && PROVIDERS.removeIf(provider -> id.equals(provider.id()));
    }

    public static CjkParagraphLayoutProvider.Layout layout(
            CjkParagraphLayoutProvider.Request request) {
        if (Boolean.TRUE.equals(DISPATCHING.get())) return null;
        DISPATCHING.set(true);
        try {
            for (CjkParagraphLayoutProvider provider : snapshot()) {
                try {
                    CjkParagraphLayoutProvider.Layout result = provider.layout(request);
                    if (result != null) return result;
                } catch (RuntimeException | LinkageError error) {
                    reportOnce(provider, "layout", error);
                }
            }
            return null;
        } finally {
            DISPATCHING.remove();
        }
    }

    public static List<ITextComponent> splitComponents(
            CjkParagraphLayoutProvider.ComponentRequest request) {
        if (Boolean.TRUE.equals(DISPATCHING.get())) return null;
        DISPATCHING.set(true);
        try {
            for (CjkParagraphLayoutProvider provider : snapshot()) {
                try {
                    List<ITextComponent> result = provider.splitComponents(request);
                    if (result != null) return result;
                } catch (RuntimeException | LinkageError error) {
                    reportOnce(provider, "component split", error);
                }
            }
            return null;
        } finally {
            DISPATCHING.remove();
        }
    }

    static synchronized void clearForTests() {
        PROVIDERS.clear();
        REPORTED_FAILURES.clear();
    }

    private static synchronized List<CjkParagraphLayoutProvider> snapshot() {
        return new ArrayList<>(PROVIDERS);
    }

    private static synchronized void reportOnce(CjkParagraphLayoutProvider provider,
                                                String operation, Throwable error) {
        String key = provider.id() + ':' + operation;
        if (REPORTED_FAILURES.add(key)) {
            LOGGER.warn("CJK paragraph provider '{}' failed during {}; using the lightweight fallback",
                    provider.id(), operation, error);
        }
    }
}
