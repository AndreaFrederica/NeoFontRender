package neofontrender.addons.command;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.command.CommandCompletionRegistration;
import neofontrender.addons.api.command.CommandCompletionResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/** Internal, side-neutral provider ordering and result-composition engine. */
public final class CommandCompletionPipeline<C> {
    private final Logger logger;
    private final CopyOnWriteArrayList<Entry<C>> providers = new CopyOnWriteArrayList<>();

    public CommandCompletionPipeline(String logName) {
        this.logger = LogManager.getLogger(logName);
    }

    public synchronized CommandCompletionRegistration register(
            ResourceLocation id, int priority, Function<C, CommandCompletionResult> provider) {
        String key = Objects.requireNonNull(id, "id").toString();
        Objects.requireNonNull(provider, "provider");
        providers.removeIf(value -> value.id.equals(key));
        Entry<C> entry = new Entry<>(key, priority, provider);
        providers.add(entry);
        providers.sort(Comparator.comparingInt((Entry<C> value) -> value.priority).reversed()
                .thenComparing(value -> value.id));
        return new CommandCompletionRegistration() {
            private boolean closed;

            @Override
            public synchronized void close() {
                if (closed) return;
                closed = true;
                providers.remove(entry);
            }
        };
    }

    public List<String> resolve(C initialContext, ContextAdapter<C> adapter) {
        List<String> initial = immutableCopy(adapter.currentCandidates(initialContext));
        List<String> current = new ArrayList<>(initial);
        boolean changed = false;

        for (Entry<C> entry : providers) {
            CommandCompletionResult result;
            try {
                C context = adapter.withCurrentCandidates(initialContext, current);
                result = Objects.requireNonNull(entry.provider.apply(context),
                        "Command completion provider returned null");
            } catch (RuntimeException | LinkageError error) {
                if (!entry.failureReported) {
                    entry.failureReported = true;
                    logger.warn("Command completion provider {} failed", entry.id, error);
                }
                continue;
            }

            switch (result.mode()) {
                case PASS:
                    break;
                case FALLBACK:
                    if (current.isEmpty()) {
                        current = merge(current, result.candidates());
                        changed = true;
                    }
                    break;
                case APPEND:
                    current = merge(current, result.candidates());
                    changed = true;
                    break;
                case REPLACE:
                    current = merge(java.util.Collections.emptyList(), result.candidates());
                    changed = true;
                    break;
                default:
                    throw new IllegalStateException("Unknown completion mode " + result.mode());
            }
        }
        return changed ? current : initial;
    }

    private static List<String> merge(Collection<String> base, Collection<String> additions) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(base);
        for (String candidate : additions) {
            if (candidate != null && !candidate.isEmpty()) merged.add(candidate);
        }
        return new ArrayList<>(merged);
    }

    private static List<String> immutableCopy(Collection<String> values) {
        return java.util.Collections.unmodifiableList(new ArrayList<>(values));
    }

    public interface ContextAdapter<C> {
        Collection<String> currentCandidates(C context);
        C withCurrentCandidates(C context, Collection<String> candidates);
    }

    private static final class Entry<C> {
        private final String id;
        private final int priority;
        private final Function<C, CommandCompletionResult> provider;
        private volatile boolean failureReported;

        private Entry(String id, int priority,
                      Function<C, CommandCompletionResult> provider) {
            this.id = id;
            this.priority = priority;
            this.provider = provider;
        }
    }
}
