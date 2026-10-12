package neofontrender.addons.controller;

/** Frame-rate-independent two-dimensional filtering and radial response for a cursor stick. */
final class ControllerCursorInputFilter {
    private float filteredX;
    private float filteredY;
    private float outputX;
    private float outputY;
    private boolean active;

    void update(float inputX, float inputY, double elapsedSeconds, float smoothing) {
        double rawX = finiteAxis(inputX);
        double rawY = finiteAxis(inputY);
        double rawLength = Math.sqrt(rawX * rawX + rawY * rawY);
        if (rawLength <= 0.01D) {
            reset();
            return;
        }
        if (rawLength > 1.0D) {
            rawX /= rawLength;
            rawY /= rawLength;
        }

        if (!active) {
            filteredX = (float) rawX;
            filteredY = (float) rawY;
            active = true;
        } else {
            double seconds = Double.isFinite(elapsedSeconds)
                    ? Math.max(0.0D, Math.min(0.05D, elapsedSeconds)) : 0.0D;
            double strength = Float.isFinite(smoothing)
                    ? Math.max(0.0D, Math.min(1.0D, smoothing)) : 0.0D;
            double timeConstant = strength * 0.08D;
            double amount = timeConstant <= 1.0E-6D
                    ? 1.0D : 1.0D - Math.exp(-seconds / timeConstant);
            filteredX += (float) ((rawX - filteredX) * amount);
            filteredY += (float) ((rawY - filteredY) * amount);
        }

        double length = Math.sqrt(filteredX * filteredX + filteredY * filteredY);
        if (length <= 1.0E-6D) {
            reset();
            return;
        }
        double easedLength = Math.min(1.0D, length * length * length);
        outputX = (float) (filteredX / length * easedLength);
        outputY = (float) (filteredY / length * easedLength);
    }

    float x() { return outputX; }
    float y() { return outputY; }

    void reset() {
        filteredX = 0.0F;
        filteredY = 0.0F;
        outputX = 0.0F;
        outputY = 0.0F;
        active = false;
    }

    private static double finiteAxis(float value) {
        return Float.isFinite(value) ? Math.max(-1.0D, Math.min(1.0D, value)) : 0.0D;
    }
}
