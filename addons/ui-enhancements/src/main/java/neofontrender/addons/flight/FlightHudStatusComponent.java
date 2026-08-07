package neofontrender.addons.flight;

import static neofontrender.addons.flight.FlightHudGraphics.*;

/** Flight-mode annunciation/status strip component. */
final class FlightHudStatusComponent implements BuiltInFlightHudComponent {
    static final FlightHudStatusComponent INSTANCE = new FlightHudStatusComponent();
    private FlightHudStatusComponent() {}

    @Override public void render(FlightHudFrame frame, FlightHudTheme.Element element) {
        float y = frame.y(element.y);
        float left = frame.x(element.x);
        float width = element.width * frame.scale;
        float cx = left + width * 0.5F;
        float ts = element.font(frame.theme) * 0.83F * frame.scale;
        int primary = primary(frame.theme);
        int selected = frame.theme.color("selected", 0xFFFF66D9);
        if (element.variant.equals("BOEING")) {
            text("ELYTRA SPD", left, y, ts, primary, halo(frame.theme));
            centeredText("HDG", cx - width * 0.16F, y, ts, primary, halo(frame.theme));
            centeredText("FLIGHT PATH", cx + width * 0.16F, y, ts, primary,
                    halo(frame.theme));
            text("MAN", left + width - textWidth("MAN", ts), y,
                    ts, selected, halo(frame.theme));
        } else if (element.variant.equals("MSFS")) {
            text(element.label.isEmpty() ? "FLIGHT INSTRUMENTS" : element.label,
                    left, y, ts, primary, halo(frame.theme));
        } else if (element.variant.startsWith("FPV")
                || element.variant.equals("TACTICAL")) {
            drawConfigurable(left, y, width, ts, frame.theme, element);
        } else if (element.variant.startsWith("AIRBUS")) {
            drawAirbus(left, y, width, ts, frame.theme, element.variant);
        } else {
            text("ELYTRA", left, y, ts, primary, halo(frame.theme));
            centeredText("MAN", cx, y, ts, selected, halo(frame.theme));
            text("FLIGHT PATH", left + width - textWidth("FLIGHT PATH", ts), y,
                    ts, primary, halo(frame.theme));
        }
    }

    private static void drawConfigurable(float left, float y, float width, float ts,
                                         FlightHudTheme theme,
                                         FlightHudTheme.Element element) {
        String leftText = dataText(element, "left", "ARMED");
        String centerText = dataText(element, "center", "ACRO");
        String rightText = dataText(element, "right", "FPV");
        int primary = primary(theme);
        int selected = theme.color("selected", 0xFFFF66D9);
        text(leftText, left, y, ts, primary, halo(theme));
        centeredText(centerText, left + width * 0.5F, y, ts,
                selected, halo(theme));
        text(rightText, left + width - textWidth(rightText, ts), y,
                ts, primary, halo(theme));
    }

    private static String dataText(FlightHudTheme.Element element,
                                   String key, String fallback) {
        return element.data.has(key) && element.data.get(key).isJsonPrimitive()
                ? element.data.get(key).getAsString() : fallback;
    }

    private static void drawAirbus(float left, float y, float width, float ts,
                                   FlightHudTheme theme, String variant) {
        int primary = primary(theme);
        int secondary = theme.color("secondary", 0xB0A9FFC0);
        int selected = theme.color("selected", 0xFFFF66D9);
        float cx = left + width * 0.5F;
        float stroke = theme.lineWidth * 0.75F;
        text("ELYTRA", left, y, ts, primary, halo(theme));
        centeredText("MAN", cx, y, ts, selected, halo(theme));
        String right = variant.endsWith("A319") ? "TRK/FPA" : "FLIGHT PATH";
        text(right, left + width - textWidth(right, ts), y, ts, primary, halo(theme));
        float underlineY = y + 10.0F * ts;
        line(cx - 13.0F * ts, underlineY, cx + 13.0F * ts, underlineY,
                selected, stroke);
        line(left + width * 0.29F, y - ts, left + width * 0.29F,
                y + 9.0F * ts, secondary, stroke);
        line(left + width * 0.71F, y - ts, left + width * 0.71F,
                y + 9.0F * ts, secondary, stroke);
    }
}
