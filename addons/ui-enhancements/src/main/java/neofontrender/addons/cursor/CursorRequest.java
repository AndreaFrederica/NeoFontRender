package neofontrender.addons.cursor;

import java.util.Objects;

/** One candidate submitted during a GUI draw frame. Higher priority wins. */
public final class CursorRequest {
    private final CursorType type;
    private final CursorInteractionState state;
    private final int priority;
    private final String source;

    public CursorRequest(CursorType type, CursorInteractionState state, int priority, String source) {
        this.type = Objects.requireNonNull(type, "type");
        this.state = Objects.requireNonNull(state, "state");
        this.priority = priority;
        this.source = source == null ? "unknown" : source;
    }

    public CursorType type() { return type; }
    public CursorInteractionState state() { return state; }
    public int priority() { return priority; }
    public String source() { return source; }

    public static CursorRequest of(CursorType type, CursorInteractionState state,
                                   int priority, String source) {
        return new CursorRequest(type, state, priority, source);
    }
}
