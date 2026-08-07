package neofontrender.addons.api.flight;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Responsive virtual-canvas mapping and live data passed to custom HUD components. */
public final class FlightHudRenderContext {
    private final Rectangle bounds;
    private final int canvasWidth;
    private final int canvasHeight;
    private final float partialTicks;
    private final FlightTelemetry telemetry;
    private final FlightState state;
    private final String themeId;
    private final String themeStyle;
    private final float lineWidth;
    private final float textScale;
    private final FlightHudCrosshairMode crosshairMode;
    private final Map<String, Integer> colors;
    private final FlightHudCanvas canvas;

    public FlightHudRenderContext(Rectangle bounds, int canvasWidth, int canvasHeight,
                                  float partialTicks, FlightTelemetry telemetry, FlightState state,
                                  String themeId, String themeStyle, float lineWidth,
                                  float textScale, FlightHudCrosshairMode crosshairMode,
                                  Map<String, Integer> colors,
                                  FlightHudCanvas canvas) {
        this.bounds = new Rectangle(bounds); this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight; this.partialTicks = partialTicks;
        this.telemetry = telemetry; this.state = state;
        this.themeId = themeId == null ? "" : themeId;
        this.themeStyle = themeStyle == null ? "" : themeStyle;
        this.lineWidth = lineWidth; this.textScale = textScale;
        this.crosshairMode = java.util.Objects.requireNonNull(crosshairMode, "crosshairMode");
        this.colors = colors == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(colors));
        this.canvas = java.util.Objects.requireNonNull(canvas, "canvas");
    }

    public Rectangle getBounds() { return new Rectangle(bounds); }
    public int getCanvasWidth() { return canvasWidth; }
    public int getCanvasHeight() { return canvasHeight; }
    public float getPartialTicks() { return partialTicks; }
    public FlightTelemetry getTelemetry() { return telemetry; }
    public FlightState getState() { return state; }
    public String getThemeId() { return themeId; }
    public String getThemeStyle() { return themeStyle; }
    public float getLineWidth() { return lineWidth; }
    public float getTextScale() { return textScale; }
    public FlightHudCrosshairMode getCrosshairMode() { return crosshairMode; }
    public Map<String, Integer> getColors() { return colors; }
    public FlightHudCanvas getCanvas() { return canvas; }
    public int getColor(String key, int fallback) {
        Integer value = colors.get(key);
        return value == null ? fallback : value;
    }
    public float getCanvasScale() { return bounds.width / (float) canvasWidth; }
    public float screenX(float canvasX) { return bounds.x + canvasX * getCanvasScale(); }
    public float screenY(float canvasY) { return bounds.y + canvasY * getCanvasScale(); }
}
