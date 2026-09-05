package neofontrender.text.syntax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** One provider's match at the current source cursor. */
public final class SyntaxMatch {
    private final int consumedLength;
    private final String emittedText;
    private final List<SyntaxOperation> operations;

    public SyntaxMatch(int consumedLength, String emittedText,
                       List<SyntaxOperation> operations) {
        if (consumedLength <= 0) throw new IllegalArgumentException("consumedLength must be positive");
        this.consumedLength = consumedLength;
        this.emittedText = emittedText == null ? "" : emittedText;
        this.operations = operations == null || operations.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(operations));
    }

    public static SyntaxMatch operations(int consumedLength, SyntaxOperation... operations) {
        return new SyntaxMatch(consumedLength, "", Arrays.asList(operations));
    }

    public int consumedLength() { return consumedLength; }
    public String emittedText() { return emittedText; }
    public List<SyntaxOperation> operations() { return operations; }
}
