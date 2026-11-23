package neofontrender.splash;

/**
 * Simple shelf allocator for the loading-screen text atlas.
 *
 * <p>The allocator only manages coordinates. When it fills up, the caller resets the whole
 * layout and reuses the same GL texture storage. This is intentional: allocating or deleting
 * textures while ModernSplash and Minecraft alternate between shared OpenGL contexts can crash
 * some Intel Windows drivers.</p>
 */
final class SplashTextureAtlasLayout {

    private final int width;
    private final int height;
    private final int gutter;
    private int cursorX;
    private int cursorY;
    private int rowHeight;

    SplashTextureAtlasLayout(int width, int height, int gutter) {
        if (width <= 0 || height <= 0 || gutter < 0) {
            throw new IllegalArgumentException("Invalid splash atlas dimensions");
        }
        this.width = width;
        this.height = height;
        this.gutter = gutter;
    }

    Placement allocate(int contentWidth, int contentHeight) {
        if (contentWidth <= 0 || contentHeight <= 0) {
            throw new IllegalArgumentException("Invalid splash atlas region dimensions");
        }

        int allocatedWidth = contentWidth + gutter * 2;
        int allocatedHeight = contentHeight + gutter * 2;
        if (allocatedWidth > width || allocatedHeight > height) {
            return null;
        }

        if (cursorX + allocatedWidth > width) {
            cursorX = 0;
            cursorY += rowHeight;
            rowHeight = 0;
        }
        if (cursorY + allocatedHeight > height) {
            return null;
        }

        Placement placement = new Placement(cursorX + gutter, cursorY + gutter,
                contentWidth, contentHeight);
        cursorX += allocatedWidth;
        rowHeight = Math.max(rowHeight, allocatedHeight);
        return placement;
    }

    void reset() {
        cursorX = 0;
        cursorY = 0;
        rowHeight = 0;
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    int gutter() {
        return gutter;
    }

    static final class Placement {
        final int x;
        final int y;
        final int width;
        final int height;

        Placement(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
