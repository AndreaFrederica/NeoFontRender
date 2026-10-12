package neofontrender.client;

import java.util.List;

/** Placement in scaled GUI coordinates, independent of Minecraft and optional menu mods. */
final class MainMenuBrandingLayout {
    static final int MARGIN = 2;

    private MainMenuBrandingLayout() {}

    static Position place(int width, int height, int textWidth, int textHeight, List<Bounds> occupied) {
        int x = MARGIN;
        int y = height - textHeight - MARGIN;
        Bounds anchor = null;
        for (Bounds bounds : occupied) {
            if (bounds.width <= 0 || bounds.height <= 0 || bounds.x < 0 || bounds.y < height / 2
                    || bounds.x >= width / 2 || bounds.y >= height) continue;
            if (anchor == null || bounds.y > anchor.y) anchor = bounds;
        }
        if (anchor != null) {
            x = anchor.x;
            y = Math.min(y, anchor.y - textHeight - MARGIN);
        }
        x = Math.max(0, Math.min(x, width - textWidth - MARGIN));
        // Each collision moves above an occupied row, so this terminates even with overlapping rows.
        boolean moved;
        do {
            moved = false;
            for (Bounds bounds : occupied) {
                if (bounds.width > 0 && bounds.height > 0 && x < bounds.x + bounds.width + MARGIN
                        && x + textWidth + MARGIN > bounds.x && y < bounds.y + bounds.height + MARGIN
                        && y + textHeight + MARGIN > bounds.y) {
                    y = bounds.y - textHeight - MARGIN;
                    moved = true;
                }
            }
        } while (moved && y >= 0);
        return y < 0 || textWidth > width || textHeight > height ? null : new Position(x, y);
    }

    static final class Bounds {
        final int x, y, width, height;
        Bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    static final class Position {
        final int x, y;
        Position(int x, int y) { this.x = x; this.y = y; }
    }
}
