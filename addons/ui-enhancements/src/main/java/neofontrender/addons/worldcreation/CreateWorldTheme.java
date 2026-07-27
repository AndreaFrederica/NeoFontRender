package neofontrender.addons.worldcreation;

import java.util.Locale;

public enum CreateWorldTheme {
    VANILLA("vanilla", false, false),
    TABBED("tabbed", true, false),
    MODERN_UI("modernui", true, true);

    private final String id;
    private final boolean tabbedLayout;
    private final boolean modernStyle;

    CreateWorldTheme(String id, boolean tabbedLayout, boolean modernStyle) {
        this.id = id;
        this.tabbedLayout = tabbedLayout;
        this.modernStyle = modernStyle;
    }

    public String id() {
        return id;
    }

    public boolean usesTabbedLayout() {
        return tabbedLayout;
    }

    public boolean usesModernStyle() {
        return modernStyle;
    }

    public static CreateWorldTheme parse(String value) {
        if (value != null) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (CreateWorldTheme theme : values()) {
                if (theme.id.equals(normalized)) return theme;
            }
        }
        return TABBED;
    }
}
