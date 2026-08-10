package neofontrender.addons.api.input;

/** Immutable timing and focus information for one resolved client input sample. */
public final class InputFrameContext {
    private final long sampleId;
    private final float partialTicks;
    private final double frameSeconds;
    private final boolean gameFocused;
    private final InputFlushReason flushReason;

    public InputFrameContext(long sampleId, float partialTicks, double frameSeconds,
                             boolean gameFocused) {
        this(sampleId, partialTicks, frameSeconds, gameFocused, null);
    }

    public InputFrameContext(long sampleId, float partialTicks, double frameSeconds,
                             boolean gameFocused, InputFlushReason flushReason) {
        this.sampleId = Math.max(0L, sampleId);
        this.partialTicks = Math.max(0.0F, Math.min(1.0F,
                Float.isFinite(partialTicks) ? partialTicks : 0.0F));
        this.frameSeconds = Double.isFinite(frameSeconds) && frameSeconds >= 0.0D
                ? frameSeconds : 0.0D;
        this.gameFocused = gameFocused;
        this.flushReason = flushReason;
    }

    public long getSampleId() { return sampleId; }
    public float getPartialTicks() { return partialTicks; }
    public double getFrameSeconds() { return frameSeconds; }
    public boolean isGameFocused() { return gameFocused; }
    public InputFlushReason getFlushReason() { return flushReason; }
}
