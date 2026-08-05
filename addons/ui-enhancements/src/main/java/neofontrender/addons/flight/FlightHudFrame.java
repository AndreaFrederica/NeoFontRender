package neofontrender.addons.flight;

import neofontrender.addons.api.flight.FlightHudRenderContext;

import java.awt.Rectangle;

/** Immutable per-frame dependencies shared by every built-in HUD component. */
final class FlightHudFrame {
    final Rectangle bounds;
    final FlightHudTheme theme;
    final FlightHudTelemetry.Sample sample;
    final float scale;
    final float roll;
    final float pitch;
    final float inputX;
    final float inputY;
    final FlightHudRenderContext publicContext;

    FlightHudFrame(Rectangle bounds, FlightHudTheme theme, FlightHudTelemetry.Sample sample,
                   float roll, float pitch, float inputX, float inputY,
                   FlightHudRenderContext publicContext) {
        this.bounds = bounds;
        this.theme = theme;
        this.sample = sample;
        this.scale = bounds.width / (float) theme.canvasWidth;
        this.roll = roll;
        this.pitch = pitch;
        this.inputX = inputX;
        this.inputY = inputY;
        this.publicContext = publicContext;
    }

    float x(float canvasX) { return bounds.x + canvasX * scale; }
    float y(float canvasY) { return bounds.y + canvasY * scale; }
}
