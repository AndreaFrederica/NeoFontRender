package neofontrender.core.font.support;

import net.minecraft.client.gui.FontRenderer;
import java.util.Collections;
import java.util.List;

/** Final tooltip geometry shared with native Forge tooltip image renderers. */
public final class TooltipLayoutCompat {
    private static final ThreadLocal<Layout> CURRENT = new ThreadLocal<>();
    private TooltipLayoutCompat() {}

    public static void publish(FontRenderer font, List<String> lines, int x, int y,
                               int width, int height) {
        CURRENT.set(new Layout(lines == null ? Collections.emptyList() : lines,
                x, y, width, height));
    }

    public static Layout current() { return CURRENT.get(); }
    public static void clear() { CURRENT.remove(); }

    public static final class Layout {
        public final List<String> lines;
        public final int x, y, width, height;
        private Layout(List<String> lines, int x, int y, int width, int height) {
            this.lines = lines; this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }
}
