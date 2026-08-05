package neofontrender.addons.flight;

import java.util.Locale;

import static neofontrender.addons.flight.FlightHudGraphics.*;

/** Shared speed/altitude tape component with reusable Airbus and Boeing drums. */
final class FlightHudTapeComponent implements BuiltInFlightHudComponent {
    static final FlightHudTapeComponent AIRSPEED = new FlightHudTapeComponent(true);
    static final FlightHudTapeComponent ALTITUDE = new FlightHudTapeComponent(false);
    private final boolean speed;

    private FlightHudTapeComponent(boolean speed) { this.speed = speed; }

    @Override public void render(FlightHudFrame frame, FlightHudTheme.Element tape) {
        double value;
        double rate;
        double limit;
        String unit;
        if (speed) {
            value = FlightHudMath.speed(frame.sample.speedBlocksPerSecond,
                    FlightRollConfig.hudSpeedUnit);
            rate = FlightHudMath.speed(frame.sample.accelerationBlocksPerSecondSquared,
                    FlightRollConfig.hudSpeedUnit);
            limit = FlightHudMath.speed(frame.sample.lowerSpeedBlocksPerSecond,
                    FlightRollConfig.hudSpeedUnit);
            unit = FlightHudMath.speedUnit(FlightRollConfig.hudSpeedUnit);
        } else {
            value = FlightHudMath.altitude(frame.sample.altitudeBlocks,
                    FlightRollConfig.hudAltitudeUnit);
            rate = FlightHudMath.altitude(frame.sample.verticalBlocksPerSecond,
                    FlightRollConfig.hudAltitudeUnit);
            limit = Double.NaN;
            unit = FlightHudMath.altitudeUnit(FlightRollConfig.hudAltitudeUnit);
        }
        drawTape(frame, tape, value, rate, limit, unit);
    }

    private void drawTape(FlightHudFrame frame, FlightHudTheme.Element tape,
                          double value, double rate, double vls, String unit) {
        float scale = frame.scale;
        FlightHudTheme theme = frame.theme;
        float railX = frame.x(tape.x);
        float top = frame.y(tape.y);
        float bottom = top + tape.height * scale;
        float center = (top + bottom) * 0.5F;
        boolean boeing = tape.variant.equals("BOEING_DRUM");
        boolean airbus = tape.variant.startsWith("AIRBUS_");
        boolean msfs = tape.variant.startsWith("MSFS_");
        double displayRange = speed
                ? FlightHudMath.speed(tape.range / 1.9438444924D,
                        FlightRollConfig.hudSpeedUnit)
                : FlightHudMath.altitude(tape.range / 3.280839895D,
                        FlightRollConfig.hudAltitudeUnit);
        double convertedMajor = speed
                ? FlightHudMath.speed(tape.majorStep / 1.9438444924D,
                        FlightRollConfig.hudSpeedUnit)
                : FlightHudMath.altitude(tape.majorStep / 3.280839895D,
                        FlightRollConfig.hudAltitudeUnit);
        double majorStep = niceStep(convertedMajor);
        double minorStep = majorStep * clamp(tape.minorStep / tape.majorStep, 0.1D, 1.0D);
        int primary = primary(theme);
        int secondary = theme.color("secondary", 0xB0A9FFC0);
        int selected = theme.color("selected", 0xFFFF66D9);
        int warning = theme.color("warning", 0xFFFF5C70);
        float stroke = tape.stroke(theme);
        if (msfs) {
            float panelLeft = speed ? railX - tape.boxWidth * scale - 12 * scale
                    : railX - Math.max(36.0F, tape.boxWidth) * scale;
            float panelRight = speed ? railX + 16 * scale
                    : railX + tape.boxWidth * scale + 16 * scale;
            quad(panelLeft, top - 19 * scale, panelRight, bottom + 16 * scale,
                    theme.color("panel", 0xB0242A31));
            outline(panelLeft, top - 19 * scale, panelRight, bottom + 16 * scale,
                    theme.color("panelBorder", 0xFF65717C), stroke);
            centeredText(speed ? "AIRSPEED" : "ALTITUDE", (panelLeft + panelRight) * 0.5F,
                    top - 16 * scale, tape.font(theme) * 0.52F * scale,
                    primary, halo(theme));
        }
        line(railX, top, railX, bottom, primary, stroke * 1.15F);
        line(railX - 5 * scale, top, railX + 5 * scale, top, primary, stroke);
        line(railX - 5 * scale, bottom, railX + 5 * scale, bottom, primary, stroke);

        double minimum = value - displayRange * 0.5D;
        double maximum = value + displayRange * 0.5D;
        double first = Math.floor(minimum / minorStep) * minorStep;
        int guard = 0;
        for (double marker = first;
             marker <= maximum + minorStep * 0.25D && guard++ < 1000;
             marker += minorStep) {
            float y = center - (float) ((marker - value) / displayRange * tape.height * scale);
            if (y < top || y > bottom) continue;
            boolean major = nearMultiple(marker, majorStep);
            float tick = (major ? 9.0F : 5.0F) * scale;
            line(railX, y, speed ? railX + tick : railX - tick, y,
                    major ? primary : secondary, stroke);
            if (major && Math.abs(y - center) > 13 * scale) {
                String label = format(marker, tape.decimals);
                float ts = tape.font(theme) * (boeing ? 0.78F : 0.72F) * scale;
                float x = speed ? railX - textWidth(label, ts) - 8 * scale
                        : railX + 8 * scale;
                if (!speed && !boeing) x = railX - textWidth(label, ts) - 10 * scale;
                text(label, x, y - 4 * scale, ts, primary, halo(theme));
            }
        }

        if (speed && Double.isFinite(vls)) drawVls(tape, value, vls, displayRange,
                railX, top, bottom, center, scale, boeing, airbus, theme, warning, stroke);

        if (boeing) drawBoeingDrum(railX, center, tape.boxWidth * scale,
                value, tape.decimals, scale, theme, tape);
        else if (airbus) drawAirbusDrum(railX, center, tape.boxWidth * scale,
                value, scale, theme, tape);
        else drawValueBox(railX, center, tape.boxWidth * scale,
                    format(value, tape.decimals), scale, theme, tape);

        float predicted = center - (float) (rate * tape.trendSeconds / displayRange
                * tape.height * scale);
        predicted = Math.max(top, Math.min(bottom, predicted));
        float trendX = railX + (speed ? 14.0F : -14.0F) * scale;
        int trendColor = airbus ? primary : selected;
        line(trendX, center, trendX, predicted, trendColor, stroke * 1.5F);
        line(trendX - 3 * scale, predicted, trendX + 3 * scale, predicted,
                trendColor, stroke * 1.5F);
        float unitTs = tape.font(theme) * 0.68F * scale;
        if (boeing) centeredText(unit, railX, top - 15 * scale,
                unitTs, primary, halo(theme));
        else text(unit, speed ? railX - 31 * scale : railX + 9 * scale,
                bottom + 5 * scale, unitTs, primary, halo(theme));
    }

    private static void drawVls(FlightHudTheme.Element tape, double value, double vls,
                                double displayRange, float railX, float top, float bottom,
                                float center, float scale, boolean boeing, boolean airbus,
                                FlightHudTheme theme, int warning, float stroke) {
        float vlsY = center - (float) ((vls - value) / displayRange * tape.height * scale);
        if (vlsY >= top && vlsY <= bottom) {
            float windowHalfHeight = (boeing ? 12.5F : airbus ? 12.5F : 9.0F)
                    * tape.scale * scale;
            if (Math.abs(vlsY - center) <= windowHalfHeight + scale) {
                float markerRight = railX - tape.boxWidth * scale - 2 * scale;
                float markerLeft = markerRight - 16 * scale;
                line(markerLeft, vlsY, markerRight, vlsY, warning, stroke * 1.6F);
                text(theme.stall.label, markerLeft, vlsY - 9 * scale,
                        tape.font(theme) * 0.67F * scale, warning, halo(theme));
            } else {
                line(railX - 25 * scale, vlsY, railX + 2 * scale, vlsY,
                        warning, stroke * 1.6F);
                text(theme.stall.label, railX - 25 * scale, vlsY - 9 * scale,
                        tape.font(theme) * 0.67F * scale, warning, halo(theme));
            }
        }
        if (value < vls) text("STALL", railX - 27 * scale, top - 13 * scale,
                tape.font(theme) * 0.72F * scale, warning, halo(theme));
    }

    private void drawValueBox(float railX, float centerY, float width, String value,
                              float scale, FlightHudTheme theme,
                              FlightHudTheme.Element element) {
        int primary = primary(theme);
        float height = 18 * element.scale * scale;
        boolean boxLeft = speed || !element.variant.equals("MSFS_RIGHT");
        float left = boxLeft ? railX - width : railX;
        float right = boxLeft ? railX : railX + width;
        outline(left, centerY - height * 0.5F, right, centerY + height * 0.5F,
                primary, element.stroke(theme) * 1.2F);
        float notch = 5 * scale;
        if (boxLeft) {
            line(right, centerY - notch, right + notch, centerY,
                    primary, element.stroke(theme) * 1.2F);
            line(right + notch, centerY, right, centerY + notch,
                    primary, element.stroke(theme) * 1.2F);
        } else {
            line(left, centerY - notch, left - notch, centerY,
                    primary, theme.lineWidth * 1.2F);
            line(left - notch, centerY, left, centerY + notch,
                    primary, theme.lineWidth * 1.2F);
        }
        float ts = element.font(theme) * 0.88F * scale;
        centeredText(value, (left + right) * 0.5F, centerY - 4.2F * scale,
                ts, primary, halo(theme));
    }

    private void drawBoeingDrum(float railX, float centerY, float width,
                                double value, int decimals, float scale,
                                FlightHudTheme theme, FlightHudTheme.Element element) {
        int primary = primary(theme);
        int secondary = theme.color("secondary", 0xB0A9FFC0);
        float height = 25 * element.scale * scale;
        float left = speed ? railX - width : railX;
        float right = speed ? railX : railX + width;
        float stroke = element.stroke(theme) * 1.2F;
        outline(left, centerY - height * 0.5F, right, centerY + height * 0.5F,
                primary, stroke);
        float pointer = 5 * scale;
        if (speed) {
            line(right, centerY - pointer, right + pointer, centerY, primary, stroke);
            line(right + pointer, centerY, right, centerY + pointer, primary, stroke);
        } else {
            line(left, centerY - pointer, left - pointer, centerY, primary, stroke);
            line(left - pointer, centerY, left, centerY + pointer, primary, stroke);
        }
        FlightHudMath.RollingDrum drum = FlightHudMath.rollingDrum(
                value, speed ? 1 : 20, speed ? 10 : 100);
        float split = right - 14 * element.scale * scale;
        line(split, centerY - height * 0.5F, split, centerY + height * 0.5F,
                secondary, element.stroke(theme));
        float mainScale = element.font(theme) * 1.08F * scale;
        FlightHudDrumRenderer.prefix(left + scale, split - scale, centerY,
                4.5F * scale, 10 * scale, mainScale, drum,
                primary, secondary, theme);
        float drumScale = element.font(theme) * (speed ? 0.8F : 0.66F) * scale;
        FlightHudDrumRenderer.rows(split + 0.7F * scale, right - 0.7F * scale,
                centerY, 4.5F * scale, 10 * scale, split + 2 * scale,
                drumScale, speed ? "%d" : "%02d", drum, primary, secondary, theme);
        if (decimals > 0) {
            double fraction = value - Math.floor(value);
            String decimal = String.format(Locale.ROOT, ".%0" + decimals + "d",
                    (int) Math.floor(fraction * Math.pow(10, decimals)));
            text(decimal, left + 2 * scale, centerY + height * 0.5F + 2 * scale,
                    element.font(theme) * 0.52F * scale, secondary, halo(theme));
        }
    }

    private void drawAirbusDrum(float railX, float centerY, float width, double value,
                                float scale, FlightHudTheme theme,
                                FlightHudTheme.Element element) {
        int primary = primary(theme);
        int secondary = theme.color("secondary", 0xB0A9FFC0);
        float stroke = element.stroke(theme) * 1.15F;
        float left = railX - width;
        float height = (speed ? 25.0F : 27.0F) * element.scale * scale;
        outline(left, centerY - height * 0.5F, railX, centerY + height * 0.5F,
                primary, stroke);
        line(railX, centerY - 5 * scale, railX + 5 * scale, centerY, primary, stroke);
        line(railX + 5 * scale, centerY, railX, centerY + 5 * scale, primary, stroke);
        FlightHudMath.RollingDrum drum = FlightHudMath.rollingDrum(
                value, speed ? 1 : 20, speed ? 10 : 100);
        float drumWidth = (speed ? 10.0F : 15.0F) * element.scale * scale;
        float split = railX - drumWidth;
        line(split, centerY - height * 0.5F, split, centerY + height * 0.5F,
                secondary, element.stroke(theme));
        float highScale = element.font(theme) * (speed ? 1.02F : 0.91F) * scale;
        FlightHudDrumRenderer.prefix(left + scale, split - scale, centerY,
                4.5F * scale, 10 * scale, highScale, drum, primary, secondary, theme);
        float drumScale = element.font(theme) * (speed ? 0.87F : 0.62F) * scale;
        FlightHudDrumRenderer.rows(split + 0.7F * scale, railX - 0.7F * scale,
                centerY, 4.5F * scale, 10 * scale, split + 2 * scale,
                drumScale, speed ? "%d" : "%02d", drum, primary, secondary, theme);
    }
}
