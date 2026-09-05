package neofontrender.text.animation;

/** Shares one animation frame across every branch of a logical text draw. */
public final class TextAnimationFrame {
    private static final ThreadLocal<State> ACTIVE = new ThreadLocal<>();

    private TextAnimationFrame() {}

    public static Scope openCurrent() {
        return open(System.currentTimeMillis() / 50L);
    }

    public static Scope open(long frameId) {
        State state = ACTIVE.get();
        if (state == null) {
            ACTIVE.set(new State(frameId, 1));
            return new Scope(true);
        }
        state.depth++;
        return new Scope(false);
    }

    public static long current() {
        State state = ACTIVE.get();
        return state == null ? System.currentTimeMillis() / 50L : state.frameId;
    }

    public static final class Scope implements AutoCloseable {
        private boolean closed;
        private final boolean owner;

        private Scope(boolean owner) {
            this.owner = owner;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            State state = ACTIVE.get();
            if (state == null) return;
            state.depth--;
            if (owner || state.depth <= 0) ACTIVE.remove();
        }
    }

    private static final class State {
        final long frameId;
        int depth;

        State(long frameId, int depth) {
            this.frameId = frameId;
            this.depth = depth;
        }
    }
}
