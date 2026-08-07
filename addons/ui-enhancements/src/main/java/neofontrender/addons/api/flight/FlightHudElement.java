package neofontrender.addons.api.flight;

import com.google.gson.JsonObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import java.util.Map;

/** Public, renderer-neutral snapshot of one schema-3 HUD element. */
public final class FlightHudElement {
    private final String id;
    private final String type;
    private final boolean enabled;
    private final float x, y, width, height, radius, scale;
    private final float range, majorStep, minorStep, boxWidth, trendSeconds;
    private final float pitchPixelsPerDegree, driftPixelsPerDegree;
    private final int pitchRange, pitchStep, decimals;
    private final FlightHudPitchMode pitchMode;
    private final boolean showBankScale, showFlightPathVector;
    private final boolean showEnergyCue, showAircraftReference;
    private final String variant, label, color;
    private final JsonObject data;

    public FlightHudElement(String id, String type, boolean enabled,
                            float x, float y, float width, float height,
                            float radius, float scale, float range,
                            float majorStep, float minorStep, float boxWidth, float trendSeconds,
                            float pitchPixelsPerDegree, float driftPixelsPerDegree,
                            int pitchRange, int pitchStep, int decimals,
                            FlightHudPitchMode pitchMode,
                            String variant, String label, String color, JsonObject data,
                            boolean showBankScale, boolean showFlightPathVector,
                            boolean showEnergyCue, boolean showAircraftReference) {
        this.id = id; this.type = type; this.enabled = enabled;
        this.x = x; this.y = y; this.width = width;
        this.height = height; this.radius = radius; this.scale = scale;
        this.range = range; this.majorStep = majorStep; this.minorStep = minorStep;
        this.boxWidth = boxWidth; this.trendSeconds = trendSeconds;
        this.pitchPixelsPerDegree = pitchPixelsPerDegree;
        this.driftPixelsPerDegree = driftPixelsPerDegree;
        this.pitchRange = pitchRange; this.pitchStep = pitchStep; this.decimals = decimals;
        this.pitchMode = java.util.Objects.requireNonNull(pitchMode, "pitchMode");
        this.variant = variant; this.label = label; this.color = color;
        this.data = copyJsonObject(data);
        this.showBankScale = showBankScale;
        this.showFlightPathVector = showFlightPathVector;
        this.showEnergyCue = showEnergyCue;
        this.showAircraftReference = showAircraftReference;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public boolean isEnabled() { return enabled; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getRadius() { return radius; }
    public float getScale() { return scale; }
    public float getRange() { return range; }
    public float getMajorStep() { return majorStep; }
    public float getMinorStep() { return minorStep; }
    public float getBoxWidth() { return boxWidth; }
    public float getTrendSeconds() { return trendSeconds; }
    public float getPitchPixelsPerDegree() { return pitchPixelsPerDegree; }
    public float getDriftPixelsPerDegree() { return driftPixelsPerDegree; }
    public int getPitchRange() { return pitchRange; }
    public int getPitchStep() { return pitchStep; }
    public int getDecimals() { return decimals; }
    public FlightHudPitchMode getPitchMode() { return pitchMode; }
    public String getVariant() { return variant; }
    public String getLabel() { return label; }
    public String getColor() { return color; }
    public JsonObject getData() { return copyJsonObject(data); }
    public boolean isBankScaleVisible() { return showBankScale; }
    public boolean isFlightPathVectorVisible() { return showFlightPathVector; }
    public boolean isEnergyCueVisible() { return showEnergyCue; }
    public boolean isAircraftReferenceVisible() { return showAircraftReference; }

    private static JsonObject copyJsonObject(JsonObject source) {
        JsonObject copy = new JsonObject();
        if (source == null) return copy;
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            copy.add(entry.getKey(), copyElement(entry.getValue()));
        }
        return copy;
    }

    private static JsonElement copyElement(JsonElement element) {
        if (element == null || element.isJsonNull()) return JsonNull.INSTANCE;
        if (element.isJsonPrimitive()) return element.getAsJsonPrimitive();
        if (element.isJsonArray()) {
            JsonArray copy = new JsonArray();
            for (JsonElement child : element.getAsJsonArray()) copy.add(copyElement(child));
            return copy;
        }
        if (element.isJsonObject()) return copyJsonObject(element.getAsJsonObject());
        return new JsonPrimitive(element.toString());
    }
}
