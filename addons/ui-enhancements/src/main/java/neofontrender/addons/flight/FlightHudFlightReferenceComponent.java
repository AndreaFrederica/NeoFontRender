package neofontrender.addons.flight;

import neofontrender.addons.api.flight.FlightHudPitchMode;

import static neofontrender.addons.flight.FlightHudGraphics.*;

/** Conformal pitch ladder, bank scale, aircraft reference, FPV and energy cue. */
final class FlightHudFlightReferenceComponent implements BuiltInFlightHudComponent {
    static final FlightHudFlightReferenceComponent INSTANCE =
            new FlightHudFlightReferenceComponent();
    private FlightHudFlightReferenceComponent() {}

    @Override public void render(FlightHudFrame frame, FlightHudTheme.Element c) {
        if (!FlightRollConfig.hudHorizon) return;
        float scale = frame.scale;
        FlightHudTheme theme = frame.theme;
        FlightHudTelemetry.Sample sample = frame.sample;
        float cx = frame.x(c.x);
        float cy = frame.y(c.y);
        float pitchScale = c.pitchPixelsPerDegree * scale;
        float halfWidth = c.width * scale * 0.5F;
        float radians = (float) Math.toRadians(-frame.roll);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        int primary = primary(theme);
        int secondary = theme.color("secondary", 0xB0A9FFC0);
        int horizon = theme.color("horizon", primary);
        float stroke = c.stroke(theme);
        boolean airbus = c.variant.startsWith("AIRBUS");

        boolean wrapPitch = c.pitchMode == FlightHudPitchMode.WRAP_360;
        int firstMarker = wrapPitch ? -180 : -c.pitchRange;
        int markerLimit = wrapPitch ? 180 : c.pitchRange + 1;
        for (int marker = firstMarker; marker < markerLimit; marker += c.pitchStep) {
            float localY = -(float) FlightHudMath.pitchLadderDelta(
                    marker, frame.pitch, wrapPitch) * pitchScale;
            if (Math.abs(localY) > c.pitchRange * pitchScale * 1.12F) continue;
            if (marker == 0 || wrapPitch && marker == -180) {
                rotatedLine(cx, cy, -halfWidth, localY, halfWidth, localY,
                        cos, sin, horizon, stroke * 1.25F);
            } else {
                float width = (marker % 10 == 0
                        ? (airbus ? 34.0F : 42.0F)
                        : (airbus ? 21.0F : 27.0F)) * scale;
                float gap = (airbus ? 22.0F : 20.0F) * scale;
                if (marker > 0) {
                    rotatedLine(cx, cy, -gap - width, localY, -gap, localY,
                            cos, sin, secondary, stroke);
                    rotatedLine(cx, cy, gap, localY, gap + width, localY,
                            cos, sin, secondary, stroke);
                } else {
                    dashedRotatedLine(cx, cy, -gap - width, localY, -gap, localY,
                            cos, sin, secondary, stroke);
                    dashedRotatedLine(cx, cy, gap, localY, gap + width, localY,
                            cos, sin, secondary, stroke);
                }
                if (marker % 10 == 0) {
                    String label = Integer.toString(marker);
                    float ts = theme.textScale * 0.65F * scale;
                    rotatedText(label, cx, cy, -gap - width - 16.0F * scale,
                            localY - 3.5F * scale, cos, sin, ts, secondary, halo(theme));
                    rotatedText(label, cx, cy, gap + width + 4.0F * scale,
                            localY - 3.5F * scale, cos, sin, ts, secondary, halo(theme));
                }
            }
        }

        if (c.showBankScale) drawBankScale(cx, cy, halfWidth, scale, theme,
                frame.roll, stroke);
        if (c.showAircraftReference) {
            drawAircraftReference(cx, cy, scale, theme, c.variant, stroke);
        }
        if (c.showFlightPathVector) {
            float fpvX = cx + (float) clamp(sample.driftAngle, -32.0D, 32.0D)
                    * c.driftPixelsPerDegree * scale;
            float fpvY = cy - (float) clamp(sample.flightPathAngle + frame.pitch,
                    -24.0D, 24.0D) * c.pitchPixelsPerDegree * scale;
            drawFlightPathVector(fpvX, fpvY, scale, theme, c.variant, stroke);
            if (c.showEnergyCue) drawEnergyCue(fpvX, fpvY, scale, theme, stroke,
                    sample.accelerationBlocksPerSecondSquared);
        }
    }

    private static void drawBankScale(float cx, float cy, float radius, float scale,
                                      FlightHudTheme theme, float roll, float stroke) {
        int primary = primary(theme);
        int selected = theme.color("selected", 0xFFFF66D9);
        float r = radius * 0.72F;
        for (int degrees = -60; degrees <= 60; degrees += 10) {
            double angle = Math.toRadians(degrees - 90.0D);
            float length = degrees % 30 == 0 ? 7.0F * scale : 4.0F * scale;
            line(cx + (float) Math.cos(angle) * (r - length),
                    cy + (float) Math.sin(angle) * (r - length),
                    cx + (float) Math.cos(angle) * r,
                    cy + (float) Math.sin(angle) * r, primary, stroke);
        }
        orientedTriangle(cx, cy - r + scale, 0.0F, 1.0F,
                5.0F * scale, primary, stroke);
        double marker = Math.toRadians(FlightRollMath.wrapDegrees(roll) - 90.0D);
        float directionX = (float) Math.cos(marker);
        float directionY = (float) Math.sin(marker);
        float mx = cx + directionX * (r - 3.0F * scale);
        float my = cy + directionY * (r - 3.0F * scale);
        orientedTriangle(mx, my, directionX, directionY,
                4.0F * scale, selected, stroke * 1.35F);
    }

    private static void drawAircraftReference(float cx, float cy, float scale,
                                              FlightHudTheme theme, String variant,
                                              float stroke) {
        int reference = theme.color("reference", 0xFFFFFF80);
        if (variant.equals("BOEING")) {
            line(cx - 31 * scale, cy, cx - 7 * scale, cy, reference, stroke * 1.2F);
            line(cx + 7 * scale, cy, cx + 31 * scale, cy, reference, stroke * 1.2F);
            line(cx - 7 * scale, cy, cx, cy + 5 * scale, reference, stroke * 1.2F);
            line(cx, cy + 5 * scale, cx + 7 * scale, cy, reference, stroke * 1.2F);
            line(cx, cy, cx, cy + 22 * scale, reference, stroke * 1.2F);
        } else if (variant.startsWith("AIRBUS")) {
            line(cx - 29 * scale, cy, cx - 12 * scale, cy, reference, stroke * 1.1F);
            line(cx - 12 * scale, cy, cx - 7 * scale, cy + 4 * scale,
                    reference, stroke * 1.1F);
            line(cx + 7 * scale, cy + 4 * scale, cx + 12 * scale, cy,
                    reference, stroke * 1.1F);
            line(cx + 12 * scale, cy, cx + 29 * scale, cy, reference, stroke * 1.1F);
            line(cx - 2.5F * scale, cy, cx + 2.5F * scale, cy, reference, stroke);
        } else {
            line(cx - 27 * scale, cy, cx - 11 * scale, cy, reference, stroke * 1.15F);
            line(cx + 11 * scale, cy, cx + 27 * scale, cy, reference, stroke * 1.15F);
        }
    }

    private static void drawFlightPathVector(float x, float y, float scale,
                                             FlightHudTheme theme, String variant,
                                             float stroke) {
        int fpv = theme.color("flightPath", 0xFF7CFFD2);
        boolean airbus = variant.startsWith("AIRBUS");
        float radius = (airbus ? 4.3F : 5.0F) * scale;
        float wing = (airbus ? 17.0F : 18.0F) * scale;
        circle(x, y, radius, fpv, stroke * 1.25F, 24);
        line(x - wing, y, x - radius, y, fpv, stroke * 1.25F);
        line(x + radius, y, x + wing, y, fpv, stroke * 1.25F);
        line(x, y + radius, x, y + (airbus ? 9.0F : 11.0F) * scale,
                fpv, stroke * 1.25F);
    }

    private static void drawEnergyCue(float x, float y, float scale,
                                      FlightHudTheme theme, float stroke,
                                      double acceleration) {
        int energy = theme.color("energy", 0xFF9DFFB5);
        float offset = (float) clamp(acceleration * 2.8D, -16.0D, 16.0D) * scale;
        float cueY = y - offset;
        float inner = 24.0F * scale;
        float outer = 34.0F * scale;
        float half = 5.0F * scale;
        line(x - outer, cueY - half, x - inner, cueY, energy, stroke);
        line(x - inner, cueY, x - outer, cueY + half, energy, stroke);
        line(x + outer, cueY - half, x + inner, cueY, energy, stroke);
        line(x + inner, cueY, x + outer, cueY + half, energy, stroke);
    }
}
