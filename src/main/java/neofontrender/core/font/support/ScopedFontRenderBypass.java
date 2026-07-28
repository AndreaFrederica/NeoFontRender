package neofontrender.core.font.support;

/** Thread-local guard used by a scoped caller that explicitly requests Minecraft's bitmap font. */
public final class ScopedFontRenderBypass {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private ScopedFontRenderBypass() {}
    public static boolean isActive() { return DEPTH.get() > 0; }
    public static void run(Runnable action) {
        DEPTH.set(DEPTH.get() + 1);
        try {
            action.run();
        } finally {
            int depth = DEPTH.get() - 1;
            if (depth == 0) DEPTH.remove(); else DEPTH.set(depth);
        }
    }
}
