package neofontrender.addons.api.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** One provider's decision and candidates. */
public final class CommandCompletionResult {
    public enum Mode {
        PASS,
        FALLBACK,
        APPEND,
        REPLACE
    }

    private static final CommandCompletionResult PASS =
            new CommandCompletionResult(Mode.PASS, Collections.emptyList());

    private final Mode mode;
    private final List<String> candidates;

    private CommandCompletionResult(Mode mode, Collection<String> candidates) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.candidates = Collections.unmodifiableList(new ArrayList<>(candidates));
    }

    public static CommandCompletionResult pass() { return PASS; }

    public static CommandCompletionResult fallback(Collection<String> candidates) {
        return of(Mode.FALLBACK, candidates);
    }

    public static CommandCompletionResult append(Collection<String> candidates) {
        return of(Mode.APPEND, candidates);
    }

    public static CommandCompletionResult replace(Collection<String> candidates) {
        return of(Mode.REPLACE, candidates);
    }

    public Mode mode() { return mode; }
    public List<String> candidates() { return candidates; }

    private static CommandCompletionResult of(Mode mode, Collection<String> candidates) {
        return new CommandCompletionResult(mode,
                Objects.requireNonNull(candidates, "candidates"));
    }
}
