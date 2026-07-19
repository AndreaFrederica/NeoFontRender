package neofontrender.core.font.layout;

public final class TextRenderRequest {

    private final String text;
    private final int argb;
    private final TextLayoutMode mode;
    private final boolean primaryStyle;
    private final boolean secondaryStyle;

    private TextRenderRequest(String text, int argb, TextLayoutMode mode, boolean primaryStyle, boolean secondaryStyle) {
        this.text = text;
        this.argb = argb;
        this.mode = mode;
        this.primaryStyle = primaryStyle;
        this.secondaryStyle = secondaryStyle;
    }

    public static TextRenderRequest plain(String text, int argb, boolean bold, boolean italic) {
        return new TextRenderRequest(text, argb, TextLayoutMode.PLAIN, bold, italic);
    }

    public static TextRenderRequest formatted(String text, int argb, boolean shadow) {
        return new TextRenderRequest(text, argb, TextLayoutMode.FORMATTED, shadow, false);
    }

    public String text() {
        return text;
    }

    public int argb() {
        return argb;
    }

    public boolean formatted() {
        return mode == TextLayoutMode.FORMATTED;
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
        if (!(obj instanceof TextRenderRequest)) {
            return false;
        }
        TextRenderRequest other = (TextRenderRequest) obj;
        return argb == other.argb
                && primaryStyle == other.primaryStyle
                && secondaryStyle == other.secondaryStyle
                && mode == other.mode
                && text.equals(other.text);
    }

    @Override
    public int hashCode() {
        int result = text.hashCode();
        result = 31 * result + argb;
        result = 31 * result + mode.hashCode();
        result = 31 * result + (primaryStyle ? 1 : 0);
        result = 31 * result + (secondaryStyle ? 1 : 0);
        return result;
    }
}