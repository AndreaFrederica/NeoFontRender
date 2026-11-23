package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;

import java.util.Collections;
import java.util.List;

/** Measured non-text content owned and positioned by the modern tooltip renderer. */
interface TooltipVisualBlock {
    int width();

    int height();

    default TooltipVisualBlock constrain(int maxWidth) {
        return this;
    }

    void draw(int x, int y, FontRenderer font);

    default String debugLabel() {
        return "visual";
    }

    /** Immutable local rectangles from the same geometry used to paint this block. */
    default List<DebugBounds> debugBounds() {
        return Collections.singletonList(new DebugBounds(debugLabel(), 0, 0, width(), height()));
    }

    final class DebugBounds {
        public final String label;
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        DebugBounds(String label, int x, int y, int width, int height) {
            this.label = label == null ? "visual" : label;
            this.x = x;
            this.y = y;
            this.width = Math.max(0, width);
            this.height = Math.max(0, height);
        }
    }
}
