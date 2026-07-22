package neofontrender.client.input.history;

/** Immutable text/cursor snapshot used by vanilla GuiTextField history. */
public final class VanillaTextState {
    public final String text;
    public final int cursor;
    public final int selection;

    public VanillaTextState(String text, int cursor, int selection) {
        this.text = text;
        this.cursor = cursor;
        this.selection = selection;
    }
}
