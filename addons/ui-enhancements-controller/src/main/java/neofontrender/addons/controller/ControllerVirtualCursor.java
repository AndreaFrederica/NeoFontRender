package neofontrender.addons.controller;

/** Small deterministic cursor integrator shared by the runtime and unit tests. */
final class ControllerVirtualCursor {
    private static final double DEFAULT_PIXELS_PER_TICK = 9.0D;

    private double previousX;
    private double previousY;
    private double x;
    private double y;
    private boolean initialized;

    void set(double x, double y, int width, int height) {
        double nextX = clamp(x, 0.0D, Math.max(0, width - 1));
        double nextY = clamp(y, 0.0D, Math.max(0, height - 1));
        this.previousX = nextX;
        this.previousY = nextY;
        this.x = nextX;
        this.y = nextY;
        this.initialized = true;
    }

    void resize(int width, int height) {
        if (!initialized) return;
        double nextX = clamp(x, 0.0D, Math.max(0, width - 1));
        double nextY = clamp(y, 0.0D, Math.max(0, height - 1));
        previousX = nextX;
        previousY = nextY;
        x = nextX;
        y = nextY;
    }

    boolean move(float axisX, float axisY, int width, int height) {
        return move(axisX, axisY, DEFAULT_PIXELS_PER_TICK, width, height);
    }

    boolean move(float axisX, float axisY, double pixelsPerTick, int width, int height) {
        double oldX = x;
        double oldY = y;
        double speed = Double.isFinite(pixelsPerTick)
                ? Math.max(0.0D, pixelsPerTick) : DEFAULT_PIXELS_PER_TICK;
        previousX = x;
        previousY = y;
        x = clamp(x + finiteAxis(axisX) * speed,
                0.0D, Math.max(0, width - 1));
        y = clamp(y + finiteAxis(axisY) * speed,
                0.0D, Math.max(0, height - 1));
        return Math.abs(x - oldX) > 1.0E-4D || Math.abs(y - oldY) > 1.0E-4D;
    }

    void attract(double targetX, double targetY, double strength, int width, int height) {
        double amount = clamp(strength, 0.0D, 1.0D);
        x = clamp(x + (targetX - x) * amount, 0.0D, Math.max(0, width - 1));
        y = clamp(y + (targetY - y) * amount, 0.0D, Math.max(0, height - 1));
    }

    int x() { return (int) Math.round(x); }
    int y() { return (int) Math.round(y); }
    double xDouble() { return x; }
    double yDouble() { return y; }
    boolean isInitialized() { return initialized; }

    int renderX(float partialTicks) {
        return (int) Math.round(renderCoordinate(previousX, x, partialTicks));
    }

    int renderY(float partialTicks) {
        return (int) Math.round(renderCoordinate(previousY, y, partialTicks));
    }

    double renderXDouble(float partialTicks) {
        return renderCoordinate(previousX, x, partialTicks);
    }

    double renderYDouble(float partialTicks) {
        return renderCoordinate(previousY, y, partialTicks);
    }

    private static double renderCoordinate(double previous, double current, float partialTicks) {
        double amount = Float.isFinite(partialTicks)
                ? clamp(partialTicks, 0.0D, 1.0D) : 0.0D;
        return previous + (current - previous) * amount;
    }

    private static double finiteAxis(float value) {
        return Float.isFinite(value) ? clamp(value, -1.0D, 1.0D) : 0.0D;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
