package neofontrender.text.animation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Shares one animation frame across every branch of a logical text draw. */
public final class TextAnimationFrame {
    private static final int AUTOMATIC_INSTANCE_LIMIT = 1024;
    private static final long AUTOMATIC_INSTANCE_TTL_MILLIS = 1_000L;
    private static final ThreadLocal<State> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SUPPRESS_NEON_OVERDRAW =
            ThreadLocal.withInitial(() -> false);
    private static final AtomicLong NEXT_INSTANCE_ID = new AtomicLong(1L);
    private static final ThreadLocal<AutomaticFrame> AUTOMATIC_FRAME =
            ThreadLocal.withInitial(AutomaticFrame::new);
    private static final Map<AutomaticKey, AutomaticInstance> AUTOMATIC_INSTANCES =
            new LinkedHashMap<AutomaticKey, AutomaticInstance>(
                    AUTOMATIC_INSTANCE_LIMIT + 1, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<AutomaticKey, AutomaticInstance> eldest) {
                    return size() > AUTOMATIC_INSTANCE_LIMIT;
                }
            };

    private TextAnimationFrame() {}

    public static Scope openCurrent() {
        long now = System.currentTimeMillis();
        return open(now / 50L, now, 0L, false);
    }

    /** Opens a logical animation instance supplied by a retained UI owner. */
    public static Scope openInstance(long instanceId) {
        long now = System.currentTimeMillis();
        return open(now / 50L, now, instanceId, instanceId > 0L);
    }

    /**
     * Supplies a compatibility identity for immediate-mode callers that only expose draw
     * coordinates. An already active explicit identity always wins.
     */
    public static Scope openAutomatic(String source, float x, float y) {
        State state = ACTIVE.get();
        if (state != null && state.instanceId > 0L) {
            return openCurrent();
        }
        long now = System.currentTimeMillis();
        AutomaticFrame frame = AUTOMATIC_FRAME.get();
        long frameId = now / 50L;
        if (frame.frameId != frameId) {
            frame.frameId = frameId;
            frame.occurrences.clear();
        }
        AutomaticBaseKey baseKey = new AutomaticBaseKey(source, x, y);
        int occurrence = frame.occurrences.merge(baseKey, 1, Integer::sum) - 1;
        long instanceId = automaticInstance(source, x, y, occurrence, now);
        return open(now / 50L, now, instanceId, true);
    }

    public static Scope open(long frameId) {
        return open(frameId, frameId * 50L, 0L, false);
    }

    /** Creates a process-unique animation identity. IDs are deliberately never reused. */
    public static long allocateInstanceId() {
        // TODO: If the allocation policy ever changes, preserve the no-reuse invariant. Reusing
        // an ID can attach a new logical text instance to an unexpired typewriter track.
        long id = NEXT_INSTANCE_ID.getAndIncrement();
        if (id > 0L) return id;
        throw new IllegalStateException("Text animation instance ID space exhausted");
    }

    public static long currentInstanceId() {
        State state = ACTIVE.get();
        return state == null ? 0L : state.instanceId;
    }

    /**
     * Suppresses the legacy repeated-glyph Neon pass while a caller captures text into an
     * alpha mask for shader post-processing. The state is thread-local so nested or unrelated
     * text draws cannot accidentally inherit it.
     */
    public static NeonSuppression suppressNeonOverdraw() {
        boolean previous = Boolean.TRUE.equals(SUPPRESS_NEON_OVERDRAW.get());
        SUPPRESS_NEON_OVERDRAW.set(true);
        return new NeonSuppression(previous);
    }

    public static boolean neonOverdrawSuppressed() {
        return Boolean.TRUE.equals(SUPPRESS_NEON_OVERDRAW.get());
    }

    public static final class NeonSuppression implements AutoCloseable {
        private final boolean previous;
        private boolean closed;

        private NeonSuppression(boolean previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            if (previous) SUPPRESS_NEON_OVERDRAW.set(true);
            else SUPPRESS_NEON_OVERDRAW.remove();
        }
    }

    private static Scope open(long frameId, long timeMillis, long instanceId,
                              boolean overrideInstance) {
        State state = ACTIVE.get();
        if (state == null) {
            ACTIVE.set(new State(frameId, timeMillis, 1,
                    overrideInstance ? instanceId : 0L));
            return new Scope(true, 0L, false);
        }
        long previousInstanceId = state.instanceId;
        state.depth++;
        if (overrideInstance) state.instanceId = instanceId;
        return new Scope(false, previousInstanceId, overrideInstance);
    }

    public static long current() {
        State state = ACTIVE.get();
        return state == null ? System.currentTimeMillis() / 50L : state.frameId;
    }

    /** Millisecond timestamp shared by every layer of one logical text draw. */
    public static long currentTimeMillis() {
        State state = ACTIVE.get();
        return state == null ? System.currentTimeMillis() : state.timeMillis;
    }

    public static final class Scope implements AutoCloseable {
        private boolean closed;
        private final boolean owner;
        private final long previousInstanceId;
        private final boolean restoreInstance;

        private Scope(boolean owner, long previousInstanceId, boolean restoreInstance) {
            this.owner = owner;
            this.previousInstanceId = previousInstanceId;
            this.restoreInstance = restoreInstance;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            State state = ACTIVE.get();
            if (state == null) return;
            if (restoreInstance) state.instanceId = previousInstanceId;
            state.depth--;
            if (owner || state.depth <= 0) ACTIVE.remove();
        }
    }

    private static final class State {
        final long frameId;
        final long timeMillis;
        int depth;
        long instanceId;

        State(long frameId, long timeMillis, int depth, long instanceId) {
            this.frameId = frameId;
            this.timeMillis = timeMillis;
            this.depth = depth;
            this.instanceId = instanceId;
        }
    }

    private static synchronized long automaticInstance(String source, float x, float y,
                                                       int occurrence, long now) {
        AutomaticKey key = new AutomaticKey(source, x, y, occurrence);
        AutomaticInstance instance = AUTOMATIC_INSTANCES.get(key);
        if (instance == null || now < instance.lastAccess
                || now - instance.lastAccess > AUTOMATIC_INSTANCE_TTL_MILLIS) {
            instance = new AutomaticInstance(allocateInstanceId(), now);
            AUTOMATIC_INSTANCES.put(key, instance);
        } else {
            instance.lastAccess = now;
        }
        return instance.id;
    }

    private static final class AutomaticInstance {
        final long id;
        long lastAccess;

        AutomaticInstance(long id, long lastAccess) {
            this.id = id;
            this.lastAccess = lastAccess;
        }
    }

    private static final class AutomaticKey {
        final String source;
        final int xBits;
        final int yBits;
        final int occurrence;

        AutomaticKey(String source, float x, float y, int occurrence) {
            this.source = source == null ? "" : source;
            this.xBits = Float.floatToIntBits(x);
            this.yBits = Float.floatToIntBits(y);
            this.occurrence = occurrence;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof AutomaticKey)) return false;
            AutomaticKey key = (AutomaticKey) other;
            return xBits == key.xBits && yBits == key.yBits && occurrence == key.occurrence
                    && source.equals(key.source);
        }

        @Override
        public int hashCode() {
            int result = source.hashCode();
            result = 31 * result + xBits;
            result = 31 * result + yBits;
            return 31 * result + occurrence;
        }
    }

    private static final class AutomaticBaseKey {
        final String source;
        final int xBits;
        final int yBits;

        AutomaticBaseKey(String source, float x, float y) {
            this.source = source == null ? "" : source;
            this.xBits = Float.floatToIntBits(x);
            this.yBits = Float.floatToIntBits(y);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof AutomaticBaseKey)) return false;
            AutomaticBaseKey key = (AutomaticBaseKey) other;
            return xBits == key.xBits && yBits == key.yBits && source.equals(key.source);
        }

        @Override
        public int hashCode() {
            int result = source.hashCode();
            result = 31 * result + xBits;
            return 31 * result + yBits;
        }
    }

    private static final class AutomaticFrame {
        long frameId = Long.MIN_VALUE;
        final Map<AutomaticBaseKey, Integer> occurrences = new LinkedHashMap<>();
    }
}
