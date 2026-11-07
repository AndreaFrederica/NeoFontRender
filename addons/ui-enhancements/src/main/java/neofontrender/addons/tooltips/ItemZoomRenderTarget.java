package neofontrender.addons.tooltips;

/** The standalone zoom surface is separate from measurement and tooltip surfaces. */
final class ItemZoomRenderTarget {
    private static final PreviewRenderTarget TARGET = new PreviewRenderTarget();

    private ItemZoomRenderTarget() {}

    static boolean render(int size, int pixelScale, Runnable draw) {
        return TARGET.render(size, size, pixelScale, draw);
    }

    static void composite(int x, int y, int size, float opacity) {
        TARGET.composite(x, y, size, size, opacity);
    }

    static void release() {
        TARGET.release();
    }
}
