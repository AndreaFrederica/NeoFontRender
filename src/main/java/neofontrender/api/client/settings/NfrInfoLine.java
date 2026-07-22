package neofontrender.api.client.settings;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Objects;
import java.util.function.Supplier;

/** One read-only line contributed to an NFR information page. */
@SideOnly(Side.CLIENT)
public final class NfrInfoLine {
    private final Supplier<String> text;
    private final int color;
    private final int gapBefore;

    private NfrInfoLine(Supplier<String> text, int color, int gapBefore) {
        this.text = Objects.requireNonNull(text, "text");
        this.color = color;
        this.gapBefore = Math.max(0, gapBefore);
    }

    public static NfrInfoLine line(String text, int color) {
        return line(() -> text, color);
    }

    public static NfrInfoLine line(Supplier<String> text, int color) {
        return new NfrInfoLine(text, color, 0);
    }

    public static NfrInfoLine spaced(String text, int color) {
        return spaced(() -> text, color);
    }

    public static NfrInfoLine spaced(Supplier<String> text, int color) {
        return new NfrInfoLine(text, color, 9);
    }

    public Supplier<String> text() { return text; }
    public int color() { return color; }
    public int gapBefore() { return gapBefore; }
}
