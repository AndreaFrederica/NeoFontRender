package neofontrender.addons.flight;

final class FlightRollMath {
    private static final double BANKING_STRENGTH = 20.0D;

    private FlightRollMath() {}

    static BankingDelta bankingDelta(float rollDegrees, float pitchDegrees,
                                     double elapsedSeconds) {
        double roll = Math.toRadians(rollDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double scale = Math.cos(pitch) * 10.0D * BANKING_STRENGTH * elapsedSeconds;
        return new BankingDelta((Math.cos(roll) - 1.0D) * scale,
                Math.sin(roll) * scale);
    }

    static final class BankingDelta {
        final double pitch;
        final double yaw;

        private BankingDelta(double pitch, double yaw) {
            this.pitch = pitch;
            this.yaw = yaw;
        }
    }

    static float clamp(float value, float maximum) {
        if (!Float.isFinite(value) || !Float.isFinite(maximum)) return 0.0F;
        float limit = Math.max(0.0F, maximum);
        return Math.max(-limit, Math.min(limit, value));
    }

    static float approach(float current, float target, float response) {
        float amount = Float.isFinite(response)
                ? Math.max(0.01F, Math.min(1.0F, response)) : 0.2F;
        return current + (target - current) * amount;
    }

    static float barrelAngle(int direction, float progress) {
        float amount = Math.max(0.0F, Math.min(1.0F, progress));
        // Smoothstep gives a controlled start/finish without copying either reference mod's math.
        float eased = amount * amount * (3.0F - 2.0F * amount);
        return Math.signum(direction) * 360.0F * eased;
    }

    static float wrapDegrees(float value) {
        if (!Float.isFinite(value)) return 0.0F;
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }
}
