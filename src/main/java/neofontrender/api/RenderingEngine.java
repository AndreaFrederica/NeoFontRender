package neofontrender.api;

import java.util.Locale;

/** Rendering backends exposed by the public API. Availability is decided during reload. */
public enum RenderingEngine {
    SFR("sfr"),
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
        if (value == null) return COSMIC;
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if ("sfr".equals(normalized) || "awt".equals(normalized)) return SFR;
        if ("cosmic".equals(normalized) || "cosmic_text".equals(normalized)) return COSMIC;
        if ("vanilla".equals(normalized) || "minecraft".equals(normalized)
                || "original".equals(normalized)) return VANILLA;
        return COSMIC;
    }
}
