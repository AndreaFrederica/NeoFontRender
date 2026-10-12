package neofontrender.addons.api.input;

/** Immutable normalized input value with button edge information. */
public final class InputValue {
    public static final InputValue NEUTRAL = new InputValue(0.0F, false, false, false);

    private final float axis;
    private final boolean down;
    private final boolean pressed;
    private final boolean released;

    public InputValue(float axis, boolean down, boolean pressed, boolean released) {
        this.axis = clampAxis(axis);
        this.down = down;
        this.pressed = pressed && down;
        this.released = released && !down;
    }

    public static InputValue axis(float value) {
        return new InputValue(value, Math.abs(value) > 1.0E-6F, false, false);
    }

    public static InputValue button(boolean down, boolean pressed, boolean released) {
        return new InputValue(down ? 1.0F : 0.0F, down, pressed, released);
    }

    public float getAxis() { return axis; }
    public boolean isDown() { return down; }
    public boolean isPressed() { return pressed; }
    public boolean isReleased() { return released; }

    private static float clampAxis(float value) {
        if (!Float.isFinite(value)) return 0.0F;
        return Math.max(-1.0F, Math.min(1.0F, value));
    }
}
