package neofontrender.api;

/** Stable public font-style values accepted by {@link FontUpdate}. */
public enum FontStyle {
    PLAIN(0),
    BOLD(1),
    ITALIC(2),
    BOLD_ITALIC(3);

    private final int configValue;

    FontStyle(int configValue) {
        this.configValue = configValue;
    }

    int configValue() {
        return configValue;
    }

    static FontStyle fromConfig(int value) {
        switch (value & 3) {
            case 1: return BOLD;
            case 2: return ITALIC;
            case 3: return BOLD_ITALIC;
            default: return PLAIN;
        }
    }
}
