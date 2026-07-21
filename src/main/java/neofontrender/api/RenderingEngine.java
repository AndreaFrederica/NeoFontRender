package neofontrender.api;

import java.util.Locale;

/** Rendering backends exposed by the public API. Availability is decided during reload. */
public enum RenderingEngine {
    SFR("sfr"),
    SKIA("skia"),
    COSMIC("cosmic"),
    VANILLA("vanilla");

    private final String configValue;

    RenderingEngine(String configValue) {
        this.configValue = configValue;
    }

    String configValue() {
        return configValue;
    }

    static RenderingEngine fromConfig(String value) {
        if (value == null) return SFR;
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if ("skia".equals(normalized) || "skija".equals(normalized)) return SKIA;
        if ("cosmic".equals(normalized) || "cosmic_text".equals(normalized)) return COSMIC;
        if ("vanilla".equals(normalized) || "minecraft".equals(normalized)
                || "original".equals(normalized)) return VANILLA;
        return SFR;
    }
}
