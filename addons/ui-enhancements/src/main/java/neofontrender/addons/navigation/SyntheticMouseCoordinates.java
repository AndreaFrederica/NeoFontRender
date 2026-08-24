package neofontrender.addons.navigation;

final class SyntheticMouseCoordinates {
    private SyntheticMouseCoordinates() {}

    static int nativeEventX(int guiX, int guiWidth, int displayWidth) {
        if (guiWidth <= 0 || displayWidth <= 0) return 0;
        int clamped = Math.max(0, Math.min(guiWidth - 1, guiX));
        return Math.max(0, Math.min(displayWidth - 1,
                (int) (((2L * clamped + 1L) * displayWidth) / (2L * guiWidth))));
    }

    static int nativeEventY(int guiY, int guiHeight, int displayHeight) {
        if (guiHeight <= 0 || displayHeight <= 0) return 0;
        int clamped = Math.max(0, Math.min(guiHeight - 1, guiY));
        int inverted = guiHeight - clamped - 1;
        return Math.max(0, Math.min(displayHeight - 1,
                (int) (((2L * inverted + 1L) * displayHeight) / (2L * guiHeight))));
    }
}
