package neofontrender.api.arc3d;

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.MathUtil;

/**
 * Stable, explicitly named access point for the Arc3D Core distributed by Neo Font Render.
 * The original {@code icyllis.arc3d.*} API remains available and is never relocated.
 */
public final class Arc3DApi {
    public static final String ARC3D_VERSION = "2026.2.0";

    private Arc3DApi() {}

    public static boolean isAvailable() {
        try {
            return MathUtil.lerp(0.0F, 1.0F, 0.5F) == 0.5F;
        } catch (LinkageError error) {
            return false;
        }
    }

    public static float lerp(float from, float to, float amount) {
        return MathUtil.lerp(from, to, amount);
    }

    public static int hsv(float hue, float saturation, float value, int alpha) {
        return (Color.HSVToColor(hue, saturation, value) & 0x00FFFFFF)
                | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    public static int lerpArgb(int from, int to, float amount) {
        amount = Math.max(0.0F, Math.min(1.0F, amount));
        return (Math.round(MathUtil.lerp(Color.alpha(from), Color.alpha(to), amount)) << 24)
                | (Math.round(MathUtil.lerp(Color.red(from), Color.red(to), amount)) << 16)
                | (Math.round(MathUtil.lerp(Color.green(from), Color.green(to), amount)) << 8)
                | Math.round(MathUtil.lerp(Color.blue(from), Color.blue(to), amount));
    }
}
