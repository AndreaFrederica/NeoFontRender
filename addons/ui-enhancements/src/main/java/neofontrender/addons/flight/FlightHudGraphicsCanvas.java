package neofontrender.addons.flight;

import neofontrender.addons.api.flight.FlightHudCanvas;

/** Public API adapter over UIE's state-safe Arc3D/GL drawing primitives. */
final class FlightHudGraphicsCanvas implements FlightHudCanvas {
    static final FlightHudGraphicsCanvas INSTANCE = new FlightHudGraphicsCanvas();
    private FlightHudGraphicsCanvas() {}

    @Override public void line(float x1, float y1, float x2, float y2,
                               int color, float width) {
        FlightHudGraphics.line(x1, y1, x2, y2, color, width);
    }
    @Override public void polyline(float[] points, int color, float width) {
        FlightHudGraphics.polyline(points, color, width);
    }
    @Override public void outline(float left, float top, float right, float bottom,
                                  int color, float width) {
        FlightHudGraphics.outline(left, top, right, bottom, color, width);
    }
    @Override public void fill(float left, float top, float right, float bottom, int color) {
        FlightHudGraphics.quad(left, top, right, bottom, color);
    }
    @Override public void triangle(float tipX, float tipY, float directionX, float directionY,
                                   float size, int color, float width) {
        FlightHudGraphics.orientedTriangle(tipX, tipY, directionX, directionY,
                size, color, width);
    }
    @Override public void diamond(float x, float y, float size, int color, float width) {
        FlightHudGraphics.diamond(x, y, size, color, width);
    }
    @Override public void circle(float x, float y, float radius, int color,
                                 float width, int segments) {
        FlightHudGraphics.circle(x, y, radius, color, width, segments);
    }
    @Override public void arc(float x, float y, float radius,
                              double startDegrees, double endDegrees,
                              int color, float width, int segments) {
        FlightHudGraphics.circleArc(x, y, radius, startDegrees, endDegrees,
                color, width, segments);
    }
    @Override public void text(String value, float x, float y, float scale,
                               int color, int haloColor) {
        FlightHudGraphics.text(value, x, y, scale, color, haloColor);
    }
    @Override public void centeredText(String value, float centerX, float y, float scale,
                                       int color, int haloColor) {
        FlightHudGraphics.centeredText(value, centerX, y, scale, color, haloColor);
    }
    @Override public float textWidth(String value, float scale) {
        return FlightHudGraphics.textWidth(value, scale);
    }
    @Override public void clip(float left, float top, float right, float bottom, Runnable draw) {
        FlightHudGraphics.withGuiScissor(left, top, right, bottom, draw);
    }
}
