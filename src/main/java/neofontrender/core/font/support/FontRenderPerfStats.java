package neofontrender.core.font.support;

import java.util.concurrent.atomic.LongAdder;

/** Low overhead counters for comparing font submission paths. */
public final class FontRenderPerfStats {
    private static final boolean ENABLED = Boolean.getBoolean("nfr.fontPerfStats");
    private static final LongAdder BATCH_FLUSHES = new LongAdder();
    private static final LongAdder BATCH_VERTICES = new LongAdder();
    private static final LongAdder BATCH_NANOS = new LongAdder();
    private static final LongAdder GLYPH_DRAWS = new LongAdder();
    private static final LongAdder GLYPH_NANOS = new LongAdder();
    private static final LongAdder SDF_DRAWS = new LongAdder();
    private static final LongAdder SDF_NANOS = new LongAdder();

    private FontRenderPerfStats() {}

    public static boolean enabled() { return ENABLED; }

    public static void recordBatchFlush(int vertices, long nanos) {
        if (!ENABLED) return;
        BATCH_FLUSHES.increment();
        BATCH_VERTICES.add(Math.max(0, vertices));
        BATCH_NANOS.add(Math.max(0L, nanos));
    }

    public static void recordGlyphDraw(long nanos) {
        if (!ENABLED) return;
        GLYPH_DRAWS.increment();
        GLYPH_NANOS.add(Math.max(0L, nanos));
    }

    public static void recordSdfDraw(long nanos) {
        if (!ENABLED) return;
        SDF_DRAWS.increment();
        SDF_NANOS.add(Math.max(0L, nanos));
    }

    public static Snapshot snapshot() {
        return new Snapshot(BATCH_FLUSHES.sum(), BATCH_VERTICES.sum(), BATCH_NANOS.sum(),
                GLYPH_DRAWS.sum(), GLYPH_NANOS.sum(), SDF_DRAWS.sum(), SDF_NANOS.sum());
    }

    public static void reset() {
        BATCH_FLUSHES.reset(); BATCH_VERTICES.reset(); BATCH_NANOS.reset();
        GLYPH_DRAWS.reset(); GLYPH_NANOS.reset(); SDF_DRAWS.reset(); SDF_NANOS.reset();
    }

    public record Snapshot(long batchFlushes, long batchVertices, long batchNanos,
                           long glyphDraws, long glyphNanos, long sdfDraws, long sdfNanos) {
        public double batchMillis() { return batchNanos / 1_000_000.0; }
        public double glyphMillis() { return glyphNanos / 1_000_000.0; }
        public double sdfMillis() { return sdfNanos / 1_000_000.0; }
    }
}
