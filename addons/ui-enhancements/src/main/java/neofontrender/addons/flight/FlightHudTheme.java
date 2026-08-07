package neofontrender.addons.flight;

import com.google.gson.JsonObject;
import neofontrender.addons.api.flight.FlightHudCrosshairMode;
import neofontrender.addons.api.flight.FlightHudElement;
import neofontrender.addons.api.flight.FlightHudPitchMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Schema-3 JSON model: the ordered element list creates and positions the complete HUD. */
final class FlightHudTheme {
    private static final Set<String> TYPES = new HashSet<>();
    static {
        String[] values = {"STATUS", "FLIGHT_REFERENCE", "AIRSPEED_TAPE", "ALTITUDE_TAPE",
                "VERTICAL_SPEED", "HEADING_RIBBON", "HEADING_ARC", "HEADING_DIAL",
                "GROUND_SPEED", "DATUM", "INPUT_STICK", "AOA_GAUGE", "ENERGY_GAUGE"};
        java.util.Collections.addAll(TYPES, values);
    }

    int schema = 3;
    String id = "custom";
    String name = "Custom HUD";
    String style = "AIRBUS_A350";
    int canvasWidth = 520;
    int canvasHeight = 300;
    FlightHudCrosshairMode crosshairMode = FlightHudCrosshairMode.HIDE_VANILLA;
    float lineWidth = 1.1F;
    float textScale = 0.7F;
    Map<String, String> colors = new LinkedHashMap<>();
    Stall stall = new Stall();
    List<Element> elements = new ArrayList<>();

    static final class Stall {
        boolean enabled = true;
        float referencePitch = -15.0F;
        float margin = 1.15F;
        String label = "VLS";
    }

    static final class Element {
        String id = "element";
        String type = "STATUS";
        boolean enabled = true;
        float x;
        float y;
        float width;
        float height;
        float radius;
        float scale = 1.0F;
        float range;
        float majorStep;
        float minorStep;
        float boxWidth;
        float trendSeconds = 5.0F;
        float pitchPixelsPerDegree = 2.2F;
        float driftPixelsPerDegree = 2.2F;
        int pitchRange = 20;
        int pitchStep = 5;
        FlightHudPitchMode pitchMode = FlightHudPitchMode.LIMITED;
        int decimals;
        String variant = "";
        String label = "";
        String color = "primary";
        JsonObject data = new JsonObject();
        boolean showBankScale = true;
        boolean showFlightPathVector = true;
        boolean showEnergyCue = true;
        boolean showAircraftReference = true;

        float stroke(FlightHudTheme theme) { return theme.lineWidth * scale; }
        float font(FlightHudTheme theme) { return theme.textScale * scale; }
    }

    void validate(String fallbackId) {
        if (schema != 3) throw new IllegalArgumentException("unsupported schema " + schema);
        if (id == null || !id.matches("[a-z0-9_.-]+(?::[a-z0-9_./-]+)?")) id = fallbackId;
        if (name == null || name.trim().isEmpty()) name = id;
        style = normalize(style, "AIRBUS_A350");
        canvasWidth = clamp(canvasWidth, 300, 1920);
        canvasHeight = clamp(canvasHeight, 180, 1080);
        if (crosshairMode == null) crosshairMode = FlightHudCrosshairMode.HIDE_VANILLA;
        lineWidth = range(lineWidth, 0.5F, 5.0F, 1.1F);
        textScale = range(textScale, 0.35F, 2.0F, 0.7F);
        if (colors == null) colors = new LinkedHashMap<>();
        if (stall == null) stall = new Stall();
        stall.referencePitch = range(stall.referencePitch, -60.0F, -5.0F, -15.0F);
        stall.margin = range(stall.margin, 1.0F, 3.0F, 1.15F);
        if (stall.label == null || stall.label.trim().isEmpty()) stall.label = "VLS";
        if (elements == null || elements.isEmpty()) {
            throw new IllegalArgumentException("HUD theme must contain at least one element");
        }
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < elements.size(); index++) {
            Element element = elements.get(index);
            if (element == null) throw new IllegalArgumentException("null HUD element at " + index);
            element.type = normalizeType(element.type);
            if (!TYPES.contains(element.type) && !element.type.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("unknown HUD element type " + element.type);
            }
            if (element.id == null || !element.id.matches("[a-z0-9_.-]+")) {
                element.id = element.type.toLowerCase(Locale.ROOT) + "-" + index;
            }
            if (!ids.add(element.id)) throw new IllegalArgumentException("duplicate element id " + element.id);
            element.x = finite(element.x, canvasWidth * 0.5F);
            element.y = finite(element.y, canvasHeight * 0.5F);
            element.width = range(element.width, 0.0F, canvasWidth * 2.0F, 0.0F);
            element.height = range(element.height, 0.0F, canvasHeight * 2.0F, 0.0F);
            element.radius = range(element.radius, 0.0F, Math.max(canvasWidth, canvasHeight), 0.0F);
            element.scale = range(element.scale, 0.2F, 4.0F, 1.0F);
            element.range = range(element.range, 0.0F, 1000000.0F, 0.0F);
            element.majorStep = range(element.majorStep, 0.0F, 1000000.0F, 0.0F);
            element.minorStep = range(element.minorStep, 0.0F, 1000000.0F, 0.0F);
            element.boxWidth = range(element.boxWidth, 0.0F, 300.0F, 0.0F);
            element.trendSeconds = range(element.trendSeconds, 0.0F, 30.0F, 5.0F);
            element.pitchPixelsPerDegree = range(element.pitchPixelsPerDegree, 0.2F, 10.0F, 2.2F);
            element.driftPixelsPerDegree = range(element.driftPixelsPerDegree, 0.2F, 10.0F, 2.2F);
            element.pitchRange = clamp(element.pitchRange, 5, 90);
            element.pitchStep = clamp(element.pitchStep, 5, 20);
            if (element.pitchMode == null) element.pitchMode = FlightHudPitchMode.LIMITED;
            element.decimals = clamp(element.decimals, 0, 2);
            element.variant = normalize(element.variant, "");
            if (element.label == null) element.label = "";
            if (element.color == null || element.color.trim().isEmpty()) element.color = "primary";
            if (element.data == null) element.data = new JsonObject();
            applyTypeDefaults(element);
        }
    }

    private static void applyTypeDefaults(Element element) {
        switch (element.type) {
            case "AIRSPEED_TAPE":
                if (element.height <= 0.0F) element.height = 100.0F;
                if (element.range <= 0.0F) element.range = 100.0F;
                if (element.majorStep <= 0.0F) element.majorStep = 20.0F;
                if (element.minorStep <= 0.0F) element.minorStep = 10.0F;
                if (element.boxWidth <= 0.0F) element.boxWidth = 32.0F;
                break;
            case "ALTITUDE_TAPE":
                if (element.height <= 0.0F) element.height = 100.0F;
                if (element.range <= 0.0F) element.range = 600.0F;
                if (element.majorStep <= 0.0F) element.majorStep = 100.0F;
                if (element.minorStep <= 0.0F) element.minorStep = 20.0F;
                if (element.boxWidth <= 0.0F) element.boxWidth = 38.0F;
                break;
            case "VERTICAL_SPEED":
                if (element.height <= 0.0F) element.height = 72.0F;
                if (element.range <= 0.0F) element.range = 2000.0F;
                break;
            case "HEADING_RIBBON":
                if (element.width <= 0.0F) element.width = 260.0F;
                headingDefaults(element, 50.0F);
                break;
            case "HEADING_ARC":
                if (element.radius <= 0.0F) element.radius = 58.0F;
                headingDefaults(element, 80.0F);
                break;
            case "HEADING_DIAL":
                if (element.radius <= 0.0F) element.radius = 42.0F;
                headingDefaults(element, 360.0F);
                break;
            case "FLIGHT_REFERENCE":
                if (element.width <= 0.0F) element.width = 200.0F;
                break;
            case "INPUT_STICK":
                if (element.radius <= 0.0F) element.radius = 7.0F;
                break;
            case "AOA_GAUGE":
                if (element.width <= 0.0F) element.width = 38.0F;
                if (element.height <= 0.0F) element.height = 82.0F;
                if (element.range <= 0.0F) element.range = 20.0F;
                break;
            case "ENERGY_GAUGE":
                if (element.width <= 0.0F) element.width = 66.0F;
                if (element.height <= 0.0F) element.height = 82.0F;
                break;
            default:
                break;
        }
    }

    private static void headingDefaults(Element element, float range) {
        if (element.range <= 0.0F) element.range = range;
        if (element.majorStep <= 0.0F) element.majorStep = 10.0F;
        if (element.minorStep <= 0.0F) element.minorStep = 5.0F;
    }

    Element first(String type) {
        String normalized = normalizeType(type);
        for (Element element : elements) if (element.enabled && element.type.equals(normalized)) return element;
        return null;
    }

    FlightHudElement publicElement(Element element) {
        return new FlightHudElement(element.id, element.type, element.enabled,
                element.x, element.y, element.width, element.height,
                element.radius, element.scale, element.range,
                element.majorStep, element.minorStep, element.boxWidth, element.trendSeconds,
                element.pitchPixelsPerDegree, element.driftPixelsPerDegree,
                element.pitchRange, element.pitchStep, element.decimals, element.pitchMode,
                element.variant, element.label, element.color, element.data,
                element.showBankScale, element.showFlightPathVector,
                element.showEnergyCue, element.showAircraftReference);
    }

    int color(String key, int fallback) {
        String value = colors.get(key);
        if (value == null) return fallback;
        try {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("#")) normalized = normalized.substring(1);
            else if (normalized.startsWith("0x")) normalized = normalized.substring(2);
            long parsed = Long.parseLong(normalized, 16);
            if (normalized.length() <= 6) parsed |= 0xFF000000L;
            return (int) parsed;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    Map<String, Integer> publicColors() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String key : colors.keySet()) result.put(key, color(key, 0xFFFFFFFF));
        return result;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeType(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.indexOf(':') >= 0 ? trimmed.toLowerCase(Locale.ROOT)
                : trimmed.toUpperCase(Locale.ROOT);
    }

    private static float finite(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private static float range(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
