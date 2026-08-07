package neofontrender.addons.flight;

import java.util.Locale;

import static neofontrender.addons.flight.FlightHudGraphics.*;

/** Small reusable readout/gauge components used by airliner and MSFS themes. */
final class FlightHudAuxiliaryComponent implements BuiltInFlightHudComponent {
    enum Kind { INPUT, GROUND_SPEED, DATUM, AOA, ENERGY }
    static final FlightHudAuxiliaryComponent INPUT = new FlightHudAuxiliaryComponent(Kind.INPUT);
    static final FlightHudAuxiliaryComponent GROUND_SPEED =
            new FlightHudAuxiliaryComponent(Kind.GROUND_SPEED);
    static final FlightHudAuxiliaryComponent DATUM = new FlightHudAuxiliaryComponent(Kind.DATUM);
    static final FlightHudAuxiliaryComponent AOA = new FlightHudAuxiliaryComponent(Kind.AOA);
    static final FlightHudAuxiliaryComponent ENERGY = new FlightHudAuxiliaryComponent(Kind.ENERGY);
    private final Kind kind;

    private FlightHudAuxiliaryComponent(Kind kind) { this.kind = kind; }

    @Override public void render(FlightHudFrame frame, FlightHudTheme.Element element) {
        switch (kind) {
            case INPUT:
                if (FlightRollConfig.hudInputIndicator) drawInput(frame, element);
                break;
            case GROUND_SPEED: drawGroundSpeed(frame, element); break;
            case DATUM: drawDatum(frame, element); break;
            case AOA: drawAoa(frame, element); break;
            case ENERGY: drawEnergy(frame, element); break;
        }
    }

    private static void drawInput(FlightHudFrame frame, FlightHudTheme.Element element) {
        float cx = frame.x(element.x);
        float cy = frame.y(element.y);
        float radius = element.radius * frame.scale;
        int secondary = frame.theme.color("secondary", 0xB0A9FFC0);
        int selected = frame.theme.color("selected", 0xFFFF66D9);
        line(cx - radius, cy, cx + radius, cy, secondary, element.stroke(frame.theme));
        line(cx, cy - radius, cx, cy + radius, secondary, element.stroke(frame.theme));
        diamond(cx + clampAxis(frame.inputX) * radius,
                cy + clampAxis(frame.inputY) * radius,
                2.5F * frame.scale, selected, element.stroke(frame.theme));
    }

    private static void drawGroundSpeed(FlightHudFrame frame, FlightHudTheme.Element element) {
        double value = FlightHudMath.speed(frame.sample.groundSpeedBlocksPerSecond,
                FlightRollConfig.hudSpeedUnit);
        String label = (element.label.isEmpty() ? "GS" : element.label)
                + " " + format(value, 0);
        text(label, frame.x(element.x), frame.y(element.y),
                element.font(frame.theme) * frame.scale,
                frame.theme.color(element.color, primary(frame.theme)), halo(frame.theme));
    }

    private static void drawDatum(FlightHudFrame frame, FlightHudTheme.Element element) {
        String label = (element.label.isEmpty() ? "Y REF" : element.label) + " "
                + FlightHudMath.altitudeUnit(FlightRollConfig.hudAltitudeUnit);
        text(label, frame.x(element.x), frame.y(element.y),
                element.font(frame.theme) * frame.scale,
                frame.theme.color(element.color, primary(frame.theme)), halo(frame.theme));
    }

    private static void drawAoa(FlightHudFrame frame, FlightHudTheme.Element element) {
        float scale = frame.scale;
        FlightHudTheme theme = frame.theme;
        float left = frame.x(element.x);
        float top = frame.y(element.y);
        float width = element.width * scale;
        float height = element.height * scale;
        int panel = theme.color("panel", 0xB0242A31);
        int primary = theme.color(element.color, 0xFFE8F6FF);
        int warning = theme.color("warning", 0xFFFFC44F);
        quad(left, top, left + width, top + height, panel);
        outline(left, top, left + width, top + height, primary, element.stroke(theme));
        centeredText(element.label.isEmpty() ? "AOA" : element.label,
                left + width * 0.5F, top + 3 * scale,
                element.font(theme) * 0.58F * scale, primary, halo(theme));
        double aoa = -frame.pitch - frame.sample.flightPathAngle;
        double range = Math.max(1.0D, element.range);
        float barBottom = top + height - 8 * scale;
        float barTop = top + 18 * scale;
        float marker = barBottom - (float) clamp((aoa + range) / (range * 2.0D),
                0.0D, 1.0D) * (barBottom - barTop);
        line(left + 7 * scale, barTop, left + 7 * scale, barBottom,
                primary, element.stroke(theme));
        line(left + 4 * scale, marker, left + width - 4 * scale, marker,
                Math.abs(aoa) > range * 0.72D ? warning : primary,
                element.stroke(theme) * 2.0F);
        centeredText(String.format(Locale.ROOT, "%+.1f°", aoa), left + width * 0.5F,
                top + height - 7 * scale, element.font(theme) * 0.5F * scale,
                primary, halo(theme));
    }

    private static void drawEnergy(FlightHudFrame frame, FlightHudTheme.Element element) {
        float scale = frame.scale;
        FlightHudTheme theme = frame.theme;
        float left = frame.x(element.x);
        float top = frame.y(element.y);
        float width = element.width * scale;
        float height = element.height * scale;
        float cx = left + width * 0.5F;
        float cy = top + height * 0.58F;
        float radius = Math.min(width, height) * 0.34F;
        int panel = theme.color("panel", 0xB0242A31);
        int primary = theme.color(element.color, 0xFFE8F6FF);
        int safe = theme.color("safe", 0xFF5DE57B);
        int warning = theme.color("warning", 0xFFFFC44F);
        quad(left, top, left + width, top + height, panel);
        outline(left, top, left + width, top + height, primary, element.stroke(theme));
        centeredText(element.label.isEmpty() ? "GLIDE" : element.label, cx,
                top + 3 * scale, element.font(theme) * 0.58F * scale,
                primary, halo(theme));
        circleArc(cx, cy, radius, 140.0D, 400.0D,
                safe, element.stroke(theme) * 2.0F, 36);
        double reference = Math.max(1.0E-5D, frame.sample.lowerSpeedBlocksPerSecond);
        double ratio = clamp(frame.sample.speedBlocksPerSecond / reference, 0.0D, 2.0D);
        double angle = Math.toRadians(140.0D + ratio / 2.0D * 260.0D);
        int needle = ratio < 1.0D ? warning : primary;
        line(cx, cy, cx + (float) Math.cos(angle) * radius * 0.82F,
                cy + (float) Math.sin(angle) * radius * 0.82F,
                needle, element.stroke(theme) * 1.5F);
        centeredText(String.format(Locale.ROOT, "%.0f%%", ratio * 100.0D), cx,
                top + height - 10 * scale, element.font(theme) * 0.55F * scale,
                primary, halo(theme));
    }
}
