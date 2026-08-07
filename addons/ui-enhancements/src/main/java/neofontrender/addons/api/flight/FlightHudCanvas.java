package neofontrender.addons.api.flight;

/**
 * Renderer-neutral immediate canvas supplied to HUD components.
 * Coordinates are screen-space scaled GUI pixels; colors are ARGB. Convert theme-canvas
 * coordinates with {@link FlightHudRenderContext#screenX(float)} and
 * {@link FlightHudRenderContext#screenY(float)}.
 */
public interface FlightHudCanvas {
    void line(float x1, float y1, float x2, float y2, int color, float width);
    void outline(float left, float top, float right, float bottom, int color, float width);
    void fill(float left, float top, float right, float bottom, int color);
    void triangle(float tipX, float tipY, float directionX, float directionY,
                  float size, int color, float width);
    void diamond(float x, float y, float size, int color, float width);
    void circle(float x, float y, float radius, int color, float width, int segments);
    void arc(float x, float y, float radius, double startDegrees, double endDegrees,
             int color, float width, int segments);
    void text(String value, float x, float y, float scale, int color, int haloColor);
    void centeredText(String value, float centerX, float y, float scale,
                      int color, int haloColor);
    float textWidth(String value, float scale);
    void clip(float left, float top, float right, float bottom, Runnable draw);
}
