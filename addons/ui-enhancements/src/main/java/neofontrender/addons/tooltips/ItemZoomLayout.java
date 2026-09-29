package neofontrender.addons.tooltips;

/** Fits a requested preview beside the inventory using the full screen height. */
final class ItemZoomLayout {
    static final int MIN_SIZE = 32;
    static final int MAX_SIZE = 512;

    final int x, y, size;

    private ItemZoomLayout(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }

    static ItemZoomLayout fit(String side, int mouseX, int screenWidth, int screenHeight,
                              int containerLeft, int containerTop, int containerWidth,
                              int containerHeight, int requestedSize, int gap, int panelInset) {
        int right = containerLeft + containerWidth;
        int leftWidth = Math.max(0, Math.min(screenWidth, containerLeft - gap));
        int rightStart = Math.max(0, right + gap);
        int rightWidth = Math.max(0, screenWidth - rightStart);
        int margin = Math.max(Math.max(2, gap), panelInset);
        boolean useLeft;
        if ("left".equals(side)) useLeft = true;
        else if ("right".equals(side)) useLeft = false;
        else if (mouseX < containerLeft) useLeft = false;
        else if (mouseX > right) useLeft = true;
        else useLeft = leftWidth * 1.1F >= rightWidth;
        int minimumWidth = MIN_SIZE + 2 * margin;
        if (useLeft && leftWidth < minimumWidth) useLeft = false;
        else if (!useLeft && rightWidth < minimumWidth) useLeft = true;
        int width = useLeft ? leftWidth : rightWidth;
        int size = Math.min(Math.max(MIN_SIZE, Math.min(MAX_SIZE, requestedSize)),
                Math.min(width - 2 * margin, screenHeight - 2 * margin));
        if (size < MIN_SIZE) return null;
        int x = (useLeft ? 0 : rightStart) + (width - size) / 2;
        int y = Math.max(margin, Math.min(screenHeight - margin - size,
                containerTop + (containerHeight - size) / 2));
        return new ItemZoomLayout(x, y, size);
    }
}
