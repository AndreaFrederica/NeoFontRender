package neofontrender.addons.hud;

import java.util.Locale;

enum HudBarTheme {
    MODERN("modern"),
    FLAT("flat"),
    GLASS("glass"),
    SEGMENTED("segmented"),
    MINIMAL("minimal"),
    CLASSIC("classic");

    final String id;

    HudBarTheme(String id) { this.id = id; }

    static HudBarTheme parse(String value) {
        if (value != null) {
            String normalized = value.toLowerCase(Locale.ROOT);
            for (HudBarTheme theme : values()) if (theme.id.equals(normalized)) return theme;
        }
        return MODERN;
    }
}
