package neofontrender.addons.flight;

import java.util.Locale;

import static neofontrender.addons.flight.FlightHudGraphics.*;

/** Vertical-speed scale component shared by the airliner layouts. */
final class FlightHudVerticalSpeedComponent implements BuiltInFlightHudComponent {
    static final FlightHudVerticalSpeedComponent INSTANCE =
            new FlightHudVerticalSpeedComponent();
    private FlightHudVerticalSpeedComponent() {}

    @Override public void render(FlightHudFrame frame, FlightHudTheme.Element vs) {
        double value = FlightHudMath.verticalRate(frame.sample.verticalBlocksPerSecond,
                FlightRollConfig.hudVerticalSpeedUnit);
        float x = frame.x(vs.x);
        float cy = frame.y(vs.y);
        float half = vs.height * frame.scale * 0.5F;
        if (frame.theme.style.startsWith("AIRBUS")) {
            drawAirbus(x, cy, half, value, frame.scale, frame.theme, vs);
            return;
        }
        int primary = primary(frame.theme);
        int selected = frame.theme.color("selected", 0xFFFF66D9);
        line(x, cy - half, x, cy + half, primary, vs.stroke(frame.theme));
        for (int i = -2; i <= 2; i++) {
            float y = cy - i * half * 0.5F;
            line(x - 3 * frame.scale, y, x + 3 * frame.scale, y,
                    primary, vs.stroke(frame.theme));
        }
        double displayRange = displayRange(vs);
        float markerY = cy - (float) clamp(value / displayRange, -1.0D, 1.0D) * half;
        line(x + 4 * frame.scale, cy, x + 16 * frame.scale, markerY,
                selected, vs.stroke(frame.theme) * 1.55F);
        text(String.format(Locale.ROOT, "%+.0f", value), x + 19 * frame.scale,
                markerY - 4 * frame.scale, vs.font(frame.theme) * 0.68F * frame.scale,
                selected, halo(frame.theme));
        drawUnit(x, cy, half, frame.scale, frame.theme, vs);
    }

    private static void drawAirbus(float x, float cy, float half, double value,
                                   float scale, FlightHudTheme theme,
                                   FlightHudTheme.Element vs) {
        int primary = primary(theme);
        int selected = theme.color("selected", 0xFFFF66D9);
        float stroke = vs.stroke(theme);
        line(x, cy - half, x, cy + half, primary, stroke * 0.75F);
        for (int i = -4; i <= 4; i++) {
            float y = cy - i * half * 0.25F;
            float tick = i == 0 || Math.abs(i) == 4 ? 5.0F : 2.5F;
            line(x - tick * scale, y, x + tick * scale, y, primary, stroke);
        }
        float markerY = cy - (float) clamp(value / displayRange(vs), -1.0D, 1.0D) * half;
        line(x + 5 * scale, cy, x + 18 * scale, markerY,
                selected, stroke * 1.45F);
        line(x + 15 * scale, markerY, x + 21 * scale, markerY,
                selected, stroke * 1.45F);
        text(String.format(Locale.ROOT, "%+.0f", value), x + 24 * scale,
                markerY - 4 * scale, vs.font(theme) * 0.62F * scale,
                selected, halo(theme));
        drawUnit(x, cy, half, scale, theme, vs);
    }

    private static double displayRange(FlightHudTheme.Element vs) {
        return "FPM".equals(FlightRollConfig.hudVerticalSpeedUnit)
                ? vs.range : vs.range / 196.8503937D;
    }

    private static void drawUnit(float x, float cy, float half, float scale,
                                 FlightHudTheme theme, FlightHudTheme.Element vs) {
        text(FlightHudMath.verticalRateUnit(FlightRollConfig.hudVerticalSpeedUnit),
                x + 7 * scale, cy + half + 5 * scale,
                vs.font(theme) * 0.54F * scale, primary(theme), halo(theme));
    }
}
