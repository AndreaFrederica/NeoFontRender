package neofontrender.core.font.layout;

public final class TextMeasureRequest {

    private final String text;
    private final TextLayoutMode mode;
    private final boolean primaryStyle;
    private final boolean secondaryStyle;

    private TextMeasureRequest(String text, TextLayoutMode mode, boolean primaryStyle, boolean secondaryStyle) {
        this.text = text;
        this.mode = mode;
        this.primaryStyle = primaryStyle;
        this.secondaryStyle = secondaryStyle;
    }

    public static TextMeasureRequest plain(String text, boolean bold, boolean italic) {
        return new TextMeasureRequest(text, TextLayoutMode.PLAIN, bold, italic);
    }

    public static TextMeasureRequest formatted(String text, boolean shadow) {
        return new TextMeasureRequest(text, TextLayoutMode.FORMATTED, shadow, false);
    }

    public String text() {
        return text;
    }

    public boolean bold() {
        return mode == TextLayoutMode.PLAIN && primaryStyle;
    }

    public boolean italic() {
        return mode == TextLayoutMode.PLAIN && secondaryStyle;
    }

    public boolean shadow() {
        return mode == TextLayoutMode.FORMATTED && primaryStyle;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TextMeasureRequest)) {
            return false;
        }
        TextMeasureRequest other = (TextMeasureRequest) obj;
        return primaryStyle == other.primaryStyle
                && secondaryStyle == other.secondaryStyle
                && mode == other.mode
                && text.equals(other.text);
    }

    @Override
    public int hashCode() {
        int result = text.hashCode();
        result = 31 * result + mode.hashCode();
        result = 31 * result + (primaryStyle ? 1 : 0);
        result = 31 * result + (secondaryStyle ? 1 : 0);
        return result;
    }
}