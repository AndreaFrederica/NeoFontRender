package neofontrender.client.input.history;

import java.awt.Point;
import java.util.List;

/** Immutable multiline text/cursor snapshot used by ModularUI text field history. */
public final class ModularTextState {
    public final List<String> text;
    public final Point main;
    public final Point offset;

    public ModularTextState(List<String> text, Point main, Point offset) {
        this.text = text;
        this.main = main;
        this.offset = offset;
    }
}
