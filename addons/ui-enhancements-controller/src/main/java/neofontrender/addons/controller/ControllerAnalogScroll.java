package neofontrender.addons.controller;

/** Converts a normalized stick axis into frame-rate-independent wheel steps. */
final class ControllerAnalogScroll {
    private static final double WHEEL_STEPS_PER_SECOND = 20.0D;

    private double remainder;
    private int direction;

    int update(float axis, double elapsedSeconds) {
        double value = Float.isFinite(axis) ? Math.max(-1.0D, Math.min(1.0D, axis)) : 0.0D;
        if (Math.abs(value) <= 0.01D) {
            reset();
            return 0;
        }

        int nextDirection = value < 0.0D ? -1 : 1;
        if (nextDirection != direction) {
            direction = nextDirection;
            remainder = 0.0D;
            return direction;
        }

        double seconds = Double.isFinite(elapsedSeconds)
                ? Math.max(0.0D, Math.min(0.05D, elapsedSeconds)) : 0.0D;
        remainder += value * WHEEL_STEPS_PER_SECOND * seconds;
        int steps = remainder < 0.0D
                ? (int) Math.ceil(remainder - 1.0E-9D)
                : (int) Math.floor(remainder + 1.0E-9D);
        remainder -= steps;
        return steps;
    }

    void reset() {
        remainder = 0.0D;
        direction = 0;
    }
}
