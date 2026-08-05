package neofontrender.addons.flight;

import java.util.Locale;

import static neofontrender.addons.flight.FlightHudGraphics.*;

/** Heading ribbon, Boeing heading arc, and MSFS-style heading dial components. */
final class FlightHudNavigationComponent implements BuiltInFlightHudComponent {
    enum Kind { RIBBON, ARC, DIAL }
    static final FlightHudNavigationComponent RIBBON =
            new FlightHudNavigationComponent(Kind.RIBBON);
    static final FlightHudNavigationComponent ARC =
            new FlightHudNavigationComponent(Kind.ARC);
    static final FlightHudNavigationComponent DIAL =
            new FlightHudNavigationComponent(Kind.DIAL);
    private final Kind kind;

    private FlightHudNavigationComponent(Kind kind) { this.kind = kind; }

    @Override public void render(FlightHudFrame frame, FlightHudTheme.Element element) {
        switch (kind) {
            case RIBBON: drawRibbon(frame, element); break;
            case ARC: drawArc(frame, element); break;
            case DIAL: drawDial(frame, element); break;
        }
    }

    private static void drawRibbon(FlightHudFrame frame, FlightHudTheme.Element h) {
        float scale = frame.scale;
        FlightHudTheme theme = frame.theme;
        float cx = frame.x(h.x);
        float y = frame.y(h.y);
        float width = h.width * scale;
        int primary = primary(theme);
        int selected = theme.color("selected", 0xFFFF66D9);
        line(cx - width * 0.5F, y, cx + width * 0.5F, y,
                primary, h.stroke(theme));
        double heading = compassHeading(frame.sample.heading);
        double first = Math.floor((heading - h.range * 0.5D) / h.minorStep) * h.minorStep;
        for (double marker = first;
             marker <= heading + h.range * 0.5D; marker += h.minorStep) {
            double delta = wrapDegrees(marker - heading);
            float x = cx + (float) (delta / h.range) * width;
            boolean major = nearMultiple(marker, h.majorStep);
            float tick = (major ? 6.0F : 3.0F) * scale;
            line(x, y, x, y + tick, primary, h.stroke(theme));
            if (major) {
                String label = String.format(Locale.ROOT, "%02d",
                        Math.floorMod((int) Math.round(marker), 360) / 10);
                centeredText(label, x, y + 8 * scale,
                        h.font(theme) * 0.62F * scale, primary, halo(theme));
            }
        }
        diamond(cx, y, 4 * scale, selected, h.stroke(theme) * 1.25F);
        if (theme.style.startsWith("AIRBUS")) {
            double drift = clamp(frame.sample.driftAngle,
                    -h.range * 0.5D, h.range * 0.5D);
            float trackX = cx + (float) (drift / h.range) * width;
            if (Math.abs(drift) >= 0.5D) {
                triangle(trackX, y - 3 * scale, 3.3F * scale,
                        false, primary, h.stroke(theme));
            } else {
                line(cx, y - 8 * scale, cx, y - 3 * scale,
                        primary, h.stroke(theme) * 1.2F);
            }
            int current = Math.floorMod((int) Math.round(heading), 360);
            int track = Math.floorMod((int) Math.round(
                    heading + frame.sample.driftAngle), 360);
            text(String.format(Locale.ROOT, "%03d/%02d", current, track / 10),
                    cx - width * 0.5F, y - 13 * scale,
                    h.font(theme) * 0.57F * scale, primary, halo(theme));
        }
    }

    private static void drawArc(FlightHudFrame frame, FlightHudTheme.Element h) {
        float scale = frame.scale;
        FlightHudTheme theme = frame.theme;
        float cx = frame.x(h.x);
        float cy = frame.y(h.y);
        float radius = h.radius * scale;
        int primary = primary(theme);
        int selected = theme.color("selected", 0xFFFF66D9);
        circleArc(cx, cy, radius, -150.0D, -30.0D,
                primary, h.stroke(theme), 36);
        double heading = compassHeading(frame.sample.heading);
        double first = Math.floor((heading - h.range * 0.5D) / h.minorStep) * h.minorStep;
        for (double marker = first;
             marker <= heading + h.range * 0.5D; marker += h.minorStep) {
            double relative = wrapDegrees(marker - heading);
            double angle = Math.toRadians(-90.0D + relative * 120.0D / h.range);
            boolean major = nearMultiple(marker, h.majorStep);
            float inner = radius - (major ? 8.0F : 4.0F) * scale;
            line(cx + (float) Math.cos(angle) * inner,
                    cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * radius,
                    cy + (float) Math.sin(angle) * radius,
                    primary, h.stroke(theme));
            if (major) {
                String label = cardinalOrTens(
                        Math.floorMod((int) Math.round(marker), 360));
                float tx = cx + (float) Math.cos(angle) * (radius - 17 * scale);
                float ty = cy + (float) Math.sin(angle) * (radius - 17 * scale);
                centeredText(label, tx, ty - 3 * scale,
                        h.font(theme) * 0.55F * scale, primary, halo(theme));
            }
        }
        triangle(cx, cy - radius - 2 * scale, 5 * scale,
                false, selected, h.stroke(theme) * 1.35F);
        centeredText(String.format(Locale.ROOT, "%03d", (int) Math.round(heading) % 360),
                cx, cy - radius + 11 * scale, h.font(theme) * 0.7F * scale,
                selected, halo(theme));
        double trackDelta = clamp(frame.sample.driftAngle,
                -h.range * 0.5D, h.range * 0.5D);
        double trackAngle = Math.toRadians(-90.0D + trackDelta * 120.0D / h.range);
        float trackX = cx + (float) Math.cos(trackAngle) * (radius + 6 * scale);
        float trackY = cy + (float) Math.sin(trackAngle) * (radius + 6 * scale);
        diamond(trackX, trackY, 3 * scale, selected, h.stroke(theme));
    }

    private static void drawDial(FlightHudFrame frame, FlightHudTheme.Element element) {
        float scale = frame.scale;
        FlightHudTheme theme = frame.theme;
        float cx = frame.x(element.x);
        float cy = frame.y(element.y);
        float radius = element.radius * scale;
        int primary = theme.color(element.color, 0xFFE8F6FF);
        int panel = theme.color("panel", 0xB0242A31);
        int selected = theme.color("selected", 0xFF53D8FF);
        disc(cx, cy, radius, panel, 64);
        circle(cx, cy, radius, primary, element.stroke(theme), 64);
        double heading = compassHeading(frame.sample.heading);
        for (int marker = 0; marker < 360; marker += 10) {
            double relative = wrapDegrees(marker - heading);
            double angle = Math.toRadians(relative - 90.0D);
            boolean major = marker % 30 == 0;
            float outer = radius - 2 * scale;
            float inner = outer - (major ? 7.0F : 4.0F) * scale;
            line(cx + (float) Math.cos(angle) * inner,
                    cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * outer,
                    cy + (float) Math.sin(angle) * outer,
                    primary, element.stroke(theme));
            if (major) {
                String label = cardinalOrTens(marker);
                float tx = cx + (float) Math.cos(angle) * (radius - 15 * scale);
                float ty = cy + (float) Math.sin(angle) * (radius - 15 * scale);
                centeredText(label, tx, ty - 3 * scale,
                        element.font(theme) * 0.62F * scale, primary, halo(theme));
            }
        }
        drawAircraftArrow(cx, cy, 10 * scale, primary, element.stroke(theme));
        double trackAngle = Math.toRadians(
                clamp(frame.sample.driftAngle, -90.0D, 90.0D) - 90.0D);
        float tx = cx + (float) Math.cos(trackAngle) * (radius - 5 * scale);
        float ty = cy + (float) Math.sin(trackAngle) * (radius - 5 * scale);
        orientedTriangle(tx, ty, (float) Math.cos(trackAngle),
                (float) Math.sin(trackAngle), 3.5F * scale,
                selected, element.stroke(theme) * 1.2F);
        centeredText(String.format(Locale.ROOT, "%03d°", (int) Math.round(heading) % 360),
                cx, cy - radius - 14 * scale, element.font(theme) * 0.68F * scale,
                primary, halo(theme));
    }

    private static String cardinalOrTens(int degrees) {
        int normalized = Math.floorMod(degrees, 360);
        if (normalized == 0) return "N";
        if (normalized == 90) return "E";
        if (normalized == 180) return "S";
        if (normalized == 270) return "W";
        return String.format(Locale.ROOT, "%02d", normalized / 10);
    }

    private static void drawAircraftArrow(float cx, float cy, float size,
                                          int color, float stroke) {
        line(cx, cy - size, cx - size * 0.45F, cy + size * 0.55F,
                color, stroke * 1.5F);
        line(cx, cy - size, cx + size * 0.45F, cy + size * 0.55F,
                color, stroke * 1.5F);
        line(cx - size * 0.9F, cy + size * 0.15F,
                cx + size * 0.9F, cy + size * 0.15F, color, stroke * 1.5F);
        line(cx, cy, cx, cy + size, color, stroke * 1.5F);
    }
}
