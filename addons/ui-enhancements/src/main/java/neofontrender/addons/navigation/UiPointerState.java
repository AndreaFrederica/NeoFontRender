package neofontrender.addons.navigation;

import neofontrender.addons.api.ui.navigation.UiInputSource;

final class UiPointerState {
    private double x;
    private double y;
    private boolean synthetic;
    private UiInputSource source;

    void move(double nextX, double nextY, UiInputSource nextSource) {
        x = nextX;
        y = nextY;
        source = nextSource;
        synthetic = true;
    }

    void physical(double nextX, double nextY) {
        x = nextX;
        y = nextY;
        synthetic = false;
        source = null;
    }

    int renderX(float partialTicks) { return (int) Math.round(x); }
    int renderY(float partialTicks) { return (int) Math.round(y); }
    boolean isSynthetic() { return synthetic; }
    UiInputSource source() { return source; }

    void clear() {
        synthetic = false;
        source = null;
    }
}
