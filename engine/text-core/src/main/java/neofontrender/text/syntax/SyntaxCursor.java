package neofontrender.text.syntax;

import java.util.Collections;
import java.util.Set;

/** Read-only parser state supplied to syntax providers. */
public final class SyntaxCursor {
    private final String source;
    private final int sourceIndex;
    private final int plainIndex;
    private final int lineStartPlainIndex;
    private final boolean lineLeading;
    private final Set<String> activeEffectGroups;

    SyntaxCursor(String source, int sourceIndex, int plainIndex, int lineStartPlainIndex,
                 boolean lineLeading, Set<String> activeEffectGroups) {
        this.source = source;
        this.sourceIndex = sourceIndex;
        this.plainIndex = plainIndex;
        this.lineStartPlainIndex = lineStartPlainIndex;
        this.lineLeading = lineLeading;
        this.activeEffectGroups = Collections.unmodifiableSet(activeEffectGroups);
    }

    public String source() { return source; }
    public int sourceIndex() { return sourceIndex; }
    public int plainIndex() { return plainIndex; }
    public int lineStartPlainIndex() { return lineStartPlainIndex; }
    public boolean lineLeading() { return lineLeading; }
    public boolean hasActiveEffectGroup(String groupId) { return activeEffectGroups.contains(groupId); }
    public int remaining() { return source.length() - sourceIndex; }
    public char charAt(int relativeIndex) { return source.charAt(sourceIndex + relativeIndex); }
}
