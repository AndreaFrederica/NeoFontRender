package neofontrender.addons.controller;

/** Time-based speed ramp for precise starts and fast sustained cursor movement. */
final class ControllerCursorMotion {
    private double speed;
    private boolean moving;

    double update(float axisX, float axisY, double elapsedSeconds,
                  double baseSpeed, double maxSpeed, double acceleration) {
        double base = finitePositive(baseSpeed, 0.0D);
        double maximum = Math.max(base, finitePositive(maxSpeed, base));
        double ramp = finitePositive(acceleration, 0.0D);
        if (!active(axisX, axisY)) {
            speed = base;
            moving = false;
            return speed;
        }
        if (!moving) {
            moving = true;
            speed = base;
            return speed;
        }
        if (speed < base || !Double.isFinite(speed)) speed = base;
        double seconds = Double.isFinite(elapsedSeconds)
                ? Math.max(0.0D, Math.min(0.05D, elapsedSeconds)) : 0.0D;
        speed = Math.min(maximum, speed + ramp * seconds);
        return speed;
    }

    void reset() {
        speed = 0.0D;
        moving = false;
    }

    private static boolean active(float x, float y) {
        return Float.isFinite(x) && Math.abs(x) > 0.01F
                || Float.isFinite(y) && Math.abs(y) > 0.01F;
    }

    private static double finitePositive(double value, double fallback) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : fallback;
    }
}
