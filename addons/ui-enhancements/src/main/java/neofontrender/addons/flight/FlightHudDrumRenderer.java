package neofontrender.addons.flight;

import java.util.Locale;

import static neofontrender.addons.flight.FlightHudGraphics.*;

/** Shared rolling-number aperture used by Airbus and Boeing tape components. */
final class FlightHudDrumRenderer {
    private FlightHudDrumRenderer() {}

    static void rows(float clipLeft, float clipRight, float centerY,
                     float clipHalfHeight, float rowStep,
                     float x, float fontScale, String format,
                     FlightHudMath.RollingDrum drum,
                     int primary, int secondary, FlightHudTheme theme) {
        float offset = (float) drum.progress * rowStep;
        float baseline = centerY - 4.2F * fontScale;
        withGuiScissor(clipLeft, centerY - clipHalfHeight,
                clipRight, centerY + clipHalfHeight, () -> {
                    text(String.format(Locale.ROOT, format, drum.previous), x,
                            baseline - rowStep - offset, fontScale, secondary, halo(theme));
                    text(String.format(Locale.ROOT, format, drum.current), x,
                            baseline - offset, fontScale, primary, halo(theme));
                    text(String.format(Locale.ROOT, format, drum.next), x,
                            baseline + rowStep - offset, fontScale, secondary, halo(theme));
                });
    }

    static void prefix(float clipLeft, float clipRight, float centerY,
                       float clipHalfHeight, float rowStep, float fontScale,
                       FlightHudMath.RollingDrum drum,
                       int primary, int secondary, FlightHudTheme theme) {
        String current = drum.prefix == 0 ? "" : Integer.toString(drum.prefix);
        float baseline = centerY - 4.2F * fontScale;
        if (drum.next != 0) {
            text(current, clipRight - textWidth(current, fontScale), baseline,
                    fontScale, primary, halo(theme));
            return;
        }
        String previous = drum.prefix <= 1 ? "" : Integer.toString(drum.prefix - 1);
        String next = Integer.toString(drum.prefix + 1);
        float offset = (float) drum.progress * rowStep;
        withGuiScissor(clipLeft, centerY - clipHalfHeight,
                clipRight, centerY + clipHalfHeight, () -> {
                    text(previous, clipRight - textWidth(previous, fontScale),
                            baseline - rowStep - offset, fontScale, secondary, halo(theme));
                    text(current, clipRight - textWidth(current, fontScale),
                            baseline - offset, fontScale, primary, halo(theme));
                    text(next, clipRight - textWidth(next, fontScale),
                            baseline + rowStep - offset, fontScale, secondary, halo(theme));
                });
    }
}
