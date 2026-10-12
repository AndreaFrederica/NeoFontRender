package neofontrender.text.edit;

import java.util.Objects;

/** Immutable source-backed editor state shared by chat and other inline editors. */
public final class SourceEditState {
    private final String text;
    private final int cursor;
    private final int selectionStart;
    private final int selectionEnd;

    public SourceEditState(String text, int cursor, int selectionStart, int selectionEnd) {
        this.text = Objects.requireNonNull(text, "text");
        this.cursor = clamp(cursor, text.length());
        this.selectionStart = clamp(selectionStart, text.length());
        this.selectionEnd = clamp(selectionEnd, text.length());
    }

    public static SourceEditState of(String text, int cursor) {
        return new SourceEditState(text, cursor, cursor, cursor);
    }

    public String text() { return text; }
    public int cursor() { return cursor; }
    public int selectionStart() { return selectionStart; }
    public int selectionEnd() { return selectionEnd; }
    public boolean hasSelection() { return selectionStart != selectionEnd; }
    public int selectionFrom() { return Math.min(selectionStart, selectionEnd); }
    public int selectionTo() { return Math.max(selectionStart, selectionEnd); }

    public boolean contains(int start, int end) {
        return start <= cursor && cursor <= end;
    }

    public SourceEditState withText(String value, int newCursor) {
        return new SourceEditState(value, newCursor, newCursor, newCursor);
    }

    private static int clamp(int value, int length) {
        return Math.max(0, Math.min(length, value));
    }
}
