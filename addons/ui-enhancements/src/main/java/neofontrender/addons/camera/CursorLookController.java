package neofontrender.addons.camera;

/** Maintains a framebuffer-space virtual cursor for cursor-driven camera aiming. */
final class CursorLookController {
    enum ControlTarget { CURSOR, CAMERA }

    private double x;
    private double y;
    private int width;
    private int height;
    private boolean initialized;
    private ControlTarget controlTarget = ControlTarget.CURSOR;

    void reset(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.x = this.width * 0.5D;
        this.y = this.height * 0.5D;
        this.initialized = true;
    }

    void update(int width, int height, int deltaX, int deltaY, double speed) {
        ensureViewport(width, height);
        double multiplier = Double.isFinite(speed) ? Math.max(0.01D, speed) : 1.0D;
        // LWJGL mouse Y grows upward; screen-space Y grows downward.
        x += deltaX * multiplier;
        y -= deltaY * multiplier;
        x = clamp(x, 0.0D, this.width - 1.0D);
        y = clamp(y, 0.0D, this.height - 1.0D);
    }

    double x() { return x; }
    double y() { return y; }
    boolean initialized() { return initialized; }
    ControlTarget controlTarget() { return controlTarget; }
    boolean controlsCamera() { return controlTarget == ControlTarget.CAMERA; }

    void toggleControlTarget() {
        controlTarget = controlsCamera() ? ControlTarget.CURSOR : ControlTarget.CAMERA;
    }

    void ensureViewport(int width, int height) {
        if (!initialized || this.width != Math.max(1, width) || this.height != Math.max(1, height)) {
            reset(width, height);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
