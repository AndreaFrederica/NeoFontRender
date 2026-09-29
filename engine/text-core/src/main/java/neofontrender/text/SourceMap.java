package neofontrender.text;

/** Maps plain-text UTF-16 boundaries back to the original source string. */
public final class SourceMap {
    private final int sourceLength;
    private final int[] starts;
    private final int[] ends;

    public SourceMap(int sourceLength, int[] starts, int[] ends) {
        if (sourceLength < 0 || starts == null || ends == null || starts.length != ends.length
                || starts.length == 0) {
            throw new IllegalArgumentException("Invalid source map");
        }
        this.sourceLength = sourceLength;
        this.starts = starts.clone();
        this.ends = ends.clone();
    }

    public int plainLength() { return starts.length - 1; }
    public int sourceLength() { return sourceLength; }
    public int sourceStart(int plainBoundary) { return starts[clamp(plainBoundary)]; }
    public int sourceEnd(int plainBoundary) { return ends[clamp(plainBoundary)]; }

    private int clamp(int boundary) {
        return Math.max(0, Math.min(plainLength(), boundary));
    }
}
