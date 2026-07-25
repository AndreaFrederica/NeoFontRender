package neofontrender.addons.loading;

/**
 * Converts the information available to a 1.12 client into monotonic local chunk readiness.
 *
 * <p>The 1.12 protocol does not transmit the server's total chunk target. The displayed value is
 * therefore the real number of received chunks divided by the client's configured view square.
 * A tiny bounded waiting pulse is used only before the first chunk arrives. The value never reaches
 * 100% until the download-terrain screen actually closes.</p>
 */
final class WorldLoadingProgress {
    private double value;
    private long lastNanos;

    void reset(long now) {
        value = 0.02D;
        lastNanos = now;
    }

    float update(int loadedChunks, int renderDistance, long startedNanos, long now) {
        int radius = Math.max(2, Math.min(16, renderDistance));
        double expected = (radius * 2.0D + 1.0D) * (radius * 2.0D + 1.0D);
        double chunks = Math.max(0.0D, loadedChunks) / expected;
        double seconds = Math.max(0.0D, (now - startedNanos) / 1_000_000_000.0D);
        double waiting = loadedChunks == 0
                ? 0.02D + 0.06D * (1.0D - Math.exp(-seconds / 1.2D))
                : 0.0D;
        double target = Math.min(0.97D, Math.max(chunks, waiting));
        double deltaSeconds = Math.max(0.0D, Math.min(0.25D, (now - lastNanos) / 1_000_000_000.0D));
        double response = 1.0D - Math.exp(-deltaSeconds * 7.0D);
        value = Math.max(value, value + (target - value) * response);
        value = Math.min(0.97D, value);
        lastNanos = now;
        return (float) value;
    }
}
