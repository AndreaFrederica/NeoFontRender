package neofontrender.core.font.support;

/**
 * Per-string error corrector for fractional glyph positioning.
 *
 * <p>When floating-point advances are rounded to screen pixels, small
 * rounding errors accumulate and cause long strings to drift. This corrector
 * tracks the deviation between the theoretical float position and the actual
 * drawn position, and injects tiny compensations (-0.25 px or +0.25 px) into
 * subsequent run positions to keep the overall string aligned.</p>
 */
public final class StringErrorCorrector {

    private float accumulatedError = 0.0f;

    /**
     * Adjusts the draw position of a text run to compensate for accumulated
     * rounding error.
     *
     * @param posX  current Minecraft-space X position (float)
     * @param width advance width of the run about to be drawn
     * @return corrected X position
     */
    public float correct(float posX, float width) {
        float target = posX + width;
        float roundedTarget = Math.round(target);
        float error = target - roundedTarget;
        accumulatedError += error;

        if (accumulatedError > 0.25f) {
            accumulatedError -= 0.25f;
            return posX - 0.25f;
        } else if (accumulatedError < -0.25f) {
            accumulatedError += 0.25f;
            return posX + 0.25f;
        }
        return posX;
    }

    public void reset() {
        accumulatedError = 0.0f;
    }
}
