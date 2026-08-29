package neofontrender.addons.cursor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stable, exception-isolated registry for third-party cursor rules. */
public final class CursorRuleRegistry {
    private static final Map<String, Entry> RULES = new LinkedHashMap<>();

    private CursorRuleRegistry() {}

    public static synchronized AutoCloseable register(String id, int priority, CursorRule rule) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_.-]+"))
            throw new IllegalArgumentException("Cursor rule id must be namespaced");
        if (rule == null) throw new IllegalArgumentException("rule must not be null");
        Entry entry = new Entry(id, priority, rule);
        RULES.put(id, entry);
        return () -> unregister(id, entry);
    }

    public static synchronized boolean unregister(String id) { return RULES.remove(id) != null; }

    static synchronized List<CursorRequest> resolve(CursorContext context) {
        List<Entry> entries = new ArrayList<>(RULES.values());
        entries.sort(Comparator.comparingInt((Entry entry) -> entry.priority).reversed()
                .thenComparing(entry -> entry.id));
        List<CursorRequest> requests = new ArrayList<>();
        for (Entry entry : entries) {
            try {
                CursorRequest request = entry.rule.resolve(context);
                if (request != null) {
                    requests.add(CursorRequest.of(request.type(), request.state(),
                            entry.priority, entry.id));
                }
            } catch (RuntimeException error) {
                CursorManager.logRuleFailure(entry.id, error);
            }
        }
        return requests;
    }

    private static synchronized void unregister(String id, Entry expected) {
        if (RULES.get(id) == expected) RULES.remove(id);
    }

    private static final class Entry {
        private final String id;
        private final int priority;
        private final CursorRule rule;

        private Entry(String id, int priority, CursorRule rule) {
            this.id = id;
            this.priority = priority;
            this.rule = rule;
        }
    }
}
