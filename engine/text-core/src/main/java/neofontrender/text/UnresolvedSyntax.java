package neofontrender.text;

/** Source range beginning with a registered trigger but claimed by no syntax provider. */
public final class UnresolvedSyntax {
    private final int sourceStart;
    private final int sourceEnd;
    private final String source;

    public UnresolvedSyntax(int sourceStart, int sourceEnd, String source) {
        this.sourceStart = sourceStart;
        this.sourceEnd = sourceEnd;
        this.source = source;
    }

    public int sourceStart() { return sourceStart; }
    public int sourceEnd() { return sourceEnd; }
    public String source() { return source; }

    @Override public String toString() {
        return '[' + Integer.toString(sourceStart) + ',' + sourceEnd + ") " + source;
    }
}
