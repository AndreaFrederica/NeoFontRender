package neofontrender.api.text.pipeline;

import java.util.Arrays;

/** Cheap first-character filter used to keep middleware dispatch off unrelated text. */
public final class TextTrigger {
    private final char[] exact;
    private final char rangeStart;
    private final char rangeEnd;
    private final boolean hasRange;

    private TextTrigger(char[] exact, char rangeStart, char rangeEnd, boolean hasRange) {
        this.exact = exact;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.hasRange = hasRange;
    }

    public static TextTrigger exact(char... characters) {
        if (characters == null || characters.length == 0) {
            throw new IllegalArgumentException("At least one trigger character is required");
        }
        char[] copy = characters.clone();
        Arrays.sort(copy);
        return new TextTrigger(copy, '\0', '\0', false);
    }

    public static TextTrigger range(char start, char end) {
        if (end < start) throw new IllegalArgumentException("Invalid trigger range");
        return new TextTrigger(new char[0], start, end, true);
    }

    public static TextTrigger exactAndRange(char[] characters, char start, char end) {
        if (end < start) throw new IllegalArgumentException("Invalid trigger range");
        char[] copy = characters == null ? new char[0] : characters.clone();
        Arrays.sort(copy);
        return new TextTrigger(copy, start, end, true);
    }

    public boolean matches(char character) {
        return Arrays.binarySearch(exact, character) >= 0
                || hasRange && character >= rangeStart && character <= rangeEnd;
    }

    char[] exactCharacters() {
        return exact;
    }

    boolean hasRange() {
        return hasRange;
    }
}
