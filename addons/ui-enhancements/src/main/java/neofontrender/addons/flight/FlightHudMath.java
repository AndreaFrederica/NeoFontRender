package neofontrender.addons.flight;

import java.util.Locale;

/** Unit conversions and warning values derived from Minecraft 1.12's Elytra travel equations. */
final class FlightHudMath {
    private static final double TICKS_PER_SECOND = 20.0D;

    private FlightHudMath() {}

    static double pitchLadderDelta(double marker, double minecraftPitch, boolean wrap360) {
        double delta = marker + minecraftPitch;
        if (!wrap360) return delta;
        delta %= 360.0D;
        if (delta >= 180.0D) delta -= 360.0D;
        if (delta < -180.0D) delta += 360.0D;
        return delta;
    }

    static double speed(double blocksPerSecond, String unit) {
        switch (normalized(unit)) {
            case "KNOTS": return blocksPerSecond * 1.9438444924D;
            case "KPH": return blocksPerSecond * 3.6D;
            case "MPS": return blocksPerSecond;
            case "BPS": return blocksPerSecond;
            default: return blocksPerSecond;
        }
    }

    static double altitude(double blocks, String unit) {
        switch (normalized(unit)) {
            case "FEET": return blocks * 3.280839895D;
            case "METERS":
            case "BLOCKS":
            default: return blocks;
        }
    }

    static String speedUnit(String unit) {
        switch (normalized(unit)) {
            case "KNOTS": return "KT";
            case "KPH": return "KM/H";
            case "MPS": return "M/S";
            default: return "B/S";
        }
    }

    static String altitudeUnit(String unit) {
        switch (normalized(unit)) {
            case "FEET": return "FT";
            case "METERS": return "M";
            default: return "BLK";
        }
    }

    static double verticalRate(double blocksPerSecond, String unit) {
        switch (normalized(unit)) {
            case "FPM": return blocksPerSecond * 3.280839895D * 60.0D;
            case "MPS":
            case "BPS":
            default: return blocksPerSecond;
        }
    }

    static String verticalRateUnit(String unit) {
        switch (normalized(unit)) {
            case "FPM": return "FT/M";
            case "MPS": return "M/S";
            default: return "B/S";
        }
    }

    /**
     * Splits a non-negative value into the fixed and rolling portions of a mechanical drum.
     * The rolling portion advances continuously from {@code current} to {@code next}; callers
     * clip the three rendered rows to the drum aperture.
     */
    static RollingDrum rollingDrum(double value, int step, int modulus) {
        if (!Double.isFinite(value) || value < 0.0D) value = 0.0D;
        if (step <= 0 || modulus <= step || modulus % step != 0) {
            throw new IllegalArgumentException("invalid rolling drum geometry");
        }
        long bucket = (long) Math.floor(value / step);
        long base = bucket * step;
        int current = Math.floorMod((int) (base % modulus), modulus);
        double progress = Math.max(0.0D, Math.min(1.0D, (value - base) / step));
        return new RollingDrum((int) (base / modulus),
                Math.floorMod(current - step, modulus), current,
                (current + step) % modulus, progress);
    }

    static final class RollingDrum {
        final int prefix;
        final int previous;
        final int current;
        final int next;
        final double progress;

        private RollingDrum(int prefix, int previous, int current, int next,
                            double progress) {
            this.prefix = prefix;
            this.previous = previous;
            this.current = current;
            this.next = next;
            this.progress = progress;
        }
    }

    /**
     * Returns the physics-derived lower-speed reference in blocks/second.
     *
     * <p>Vanilla applies {@code -0.08 + cos(pitch)^2 * 0.06} vertical acceleration and, while
     * pitching up, converts horizontal velocity to vertical acceleration with
     * {@code horizontalSpeed * -sin(pitch) * 0.04 * 3.2}. Solving those terms for the horizontal
     * speed that can offset the gravity deficit gives a useful VLS-style warning line. It is not
     * a real aerodynamic stall model because vanilla has no wing angle-of-attack state.</p>
     */
    static double vanillaElytraLowerSpeed(float currentPitch, float referencePitch, float margin) {
        float pitch = currentPitch < referencePitch ? currentPitch : referencePitch;
        pitch = Math.max(-60.0F, Math.min(-5.0F, pitch));
        double radians = Math.toRadians(pitch);
        double cos = Math.cos(radians);
        double gravityDeficit = 0.08D - cos * cos * 0.06D;
        double pitchConversion = -Math.sin(radians) * 0.04D * 3.2D;
        double blocksPerTick = pitchConversion <= 1.0E-6D ? 0.0D
                : gravityDeficit / pitchConversion;
        return Math.max(0.0D, blocksPerTick * TICKS_PER_SECOND
                * Math.max(1.0F, Math.min(3.0F, margin)));
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
