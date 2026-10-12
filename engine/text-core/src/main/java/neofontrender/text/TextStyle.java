package neofontrender.text;

import java.util.Objects;

/** Immutable renderer-independent style state for one structured-text span. */
public final class TextStyle {
    public static final TextStyle DEFAULT = new TextStyle(false, 0, false,
            false, false, false, false);

    private final boolean colorOverride;
    private final int rgb;
    private final boolean obfuscated;
    private final boolean bold;
    private final boolean italic;
    private final boolean underline;
    private final boolean strikethrough;

    private TextStyle(boolean colorOverride, int rgb, boolean obfuscated, boolean bold,
                      boolean italic, boolean underline, boolean strikethrough) {
        this.colorOverride = colorOverride;
        this.rgb = rgb & 0xFFFFFF;
        this.obfuscated = obfuscated;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strikethrough = strikethrough;
    }

    public boolean hasColorOverride() { return colorOverride; }
    public int rgb() { return rgb; }
    public boolean obfuscated() { return obfuscated; }
    public boolean bold() { return bold; }
    public boolean italic() { return italic; }
    public boolean underline() { return underline; }
    public boolean strikethrough() { return strikethrough; }

    public TextStyle withColor(int value) {
        return new TextStyle(true, value, false, false, false, false, false);
    }

    /** Applies an external run color without changing already parsed style flags. */
    public TextStyle withColorOverride(int value) {
        return copy(true, value, obfuscated, bold, italic, underline, strikethrough);
    }

    public TextStyle withObfuscated(boolean value) {
        return copy(colorOverride, rgb, value, bold, italic, underline, strikethrough);
    }

    public TextStyle withBold(boolean value) {
        return copy(colorOverride, rgb, obfuscated, value, italic, underline, strikethrough);
    }

    public TextStyle withItalic(boolean value) {
        return copy(colorOverride, rgb, obfuscated, bold, value, underline, strikethrough);
    }

    public TextStyle withUnderline(boolean value) {
        return copy(colorOverride, rgb, obfuscated, bold, italic, value, strikethrough);
    }

    public TextStyle withStrikethrough(boolean value) {
        return copy(colorOverride, rgb, obfuscated, bold, italic, underline, value);
    }

    private TextStyle copy(boolean nextColorOverride, int nextRgb, boolean nextObfuscated,
                           boolean nextBold, boolean nextItalic, boolean nextUnderline,
                           boolean nextStrikethrough) {
        TextStyle result = new TextStyle(nextColorOverride, nextRgb, nextObfuscated, nextBold,
                nextItalic, nextUnderline, nextStrikethrough);
        return equals(result) ? this : result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TextStyle)) return false;
        TextStyle style = (TextStyle) other;
        return colorOverride == style.colorOverride && rgb == style.rgb
                && obfuscated == style.obfuscated && bold == style.bold
                && italic == style.italic && underline == style.underline
                && strikethrough == style.strikethrough;
    }

    @Override
    public int hashCode() {
        return Objects.hash(colorOverride, rgb, obfuscated, bold, italic,
                underline, strikethrough);
    }

    @Override
    public String toString() {
        return "TextStyle{" + (colorOverride ? String.format("#%06X", rgb) : "base")
                + (obfuscated ? ", obfuscated" : "") + (bold ? ", bold" : "")
                + (italic ? ", italic" : "") + (underline ? ", underline" : "")
                + (strikethrough ? ", strikethrough" : "") + '}';
    }
}
