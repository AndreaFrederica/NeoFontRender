package neofontrender.addons.controller.sdl;

public final class AxisNormalizer {
    private AxisNormalizer() {}

    public static float normalize(short value, float deadzone) {
        float axis = value < 0 ? value / 32768.0F : value / 32767.0F;
        return applyDeadzone(axis, deadzone);
    }

    public static float applyDeadzone(float axis, float deadzone) {
        if (!Float.isFinite(axis)) return 0.0F;
        float clampedAxis = Math.max(-1.0F, Math.min(1.0F, axis));
        float clampedDeadzone = Math.max(0.0F, Math.min(0.99F,
                Float.isFinite(deadzone) ? deadzone : 0.0F));
        float magnitude = Math.abs(clampedAxis);
        if (magnitude <= clampedDeadzone) return 0.0F;
        return Math.copySign((magnitude - clampedDeadzone) / (1.0F - clampedDeadzone),
                clampedAxis);
    }
}
