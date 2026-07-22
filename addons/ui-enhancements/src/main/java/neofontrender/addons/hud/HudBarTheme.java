package neofontrender.addons.hud;

import java.util.Locale;

/** Supported visual treatments for every registered status bar. */
enum HudBarTheme {
    CLASSIC("classic"),
    MODERN("modern"),
    FLAT("flat"),
    GLASS("glass"),
    SEGMENTED("segmented"),
    MINIMAL("minimal");

    final String id;

    HudBarTheme(String id) {
        this.id = id;
    }

    static HudBarTheme parse(String value) {
        if (value == null) return MODERN;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (HudBarTheme theme : values()) {
            if (theme.id.equals(normalized)) return theme;
        }
        return MODERN;
    }
}
