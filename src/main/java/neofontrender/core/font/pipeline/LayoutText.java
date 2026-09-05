package neofontrender.core.font.pipeline;

import neofontrender.core.font.pipeline.builtin.TinkersAntiqueSyntaxProvider;
import neofontrender.text.StructuredText;
import neofontrender.text.TextStyle;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A paragraph-layout view of formatted text.
 *
 * <p>Protocol markers and Minecraft formatting codes are removed from {@link #visibleText()},
 * while raw-string boundaries and the draw state at every visible position are retained. Layout
 * engines can therefore shape only visible glyphs and reconstruct independently drawn runs with
 * {@link #formattedDisplay(int, String)}.</p>
 */
public final class LayoutText {
    private final String rawText;
    private final String visibleText;
    private final int[] rawStarts;
    private final int[] rawEnds;
    private final TreeMap<Integer, State> states;
    private final boolean transformed;
    private final boolean tinkersRgbEncoding;

    private LayoutText(String rawText, String visibleText, int[] rawStarts, int[] rawEnds,
                       TreeMap<Integer, State> states, boolean transformed,
                       boolean tinkersRgbEncoding) {
        this.rawText = rawText;
        this.visibleText = visibleText;
        this.rawStarts = rawStarts;
        this.rawEnds = rawEnds;
        this.states = states;
        this.transformed = transformed;
        this.tinkersRgbEncoding = tinkersRgbEncoding;
    }

    public static LayoutText process(String rawText) {
        String raw = rawText == null ? "" : rawText;
        return fromStructured(StructuredTextRuntime.parse(raw));
    }

    public static LayoutText fromStructured(StructuredText structured) {
        Objects.requireNonNull(structured, "structured");
        int length = structured.plainText().length();
        int[] starts = new int[length + 1];
        int[] ends = new int[length + 1];
        for (int index = 0; index <= length; index++) {
            starts[index] = structured.sourceMap().sourceStart(index);
            ends[index] = structured.sourceMap().sourceEnd(index);
        }
        boolean transformed = !structured.sourceText().equals(structured.plainText())
                || !structured.inlineSpans().isEmpty() || !structured.effects().isEmpty();
        return new LayoutText(structured.sourceText(), structured.plainText(), starts, ends,
                drawStates(structured), transformed,
                TinkersAntiqueSyntaxProvider.INSTANCE.isEnabled());
    }

    public String rawText() { return rawText; }
    public String visibleText() { return visibleText; }
    public boolean transformed() { return transformed; }

    /** Stable cache discriminator for layout-visible text, source boundaries and draw states. */
    public int fingerprint() {
        int result = Objects.hash(visibleText, states, transformed, tinkersRgbEncoding);
        result = 31 * result + Arrays.hashCode(rawStarts);
        return 31 * result + Arrays.hashCode(rawEnds);
    }

    public int rawStartBoundary(int visibleOffset) {
        return rawStarts[clamp(visibleOffset)];
    }

    public int rawEndBoundary(int visibleOffset) {
        return rawEnds[clamp(visibleOffset)];
    }

    public State stateAt(int visibleOffset) {
        java.util.Map.Entry<Integer, State> entry = states.floorEntry(clamp(visibleOffset));
        return entry == null ? State.EMPTY : entry.getValue();
    }

    /** Prefixes an independently drawn layout run with its RGB and legacy formatting state. */
    public String formattedDisplay(int visibleStart, String displayText) {
        State state = stateAt(visibleStart);
        StringBuilder result = new StringBuilder(displayText.length() + 16);
        if (state.hasRgbOverride()) {
            appendRgbMarker(result, state.rgb());
        }
        return result.append(state.prefix())
                .append(Objects.requireNonNull(displayText, "displayText")).toString();
    }

    private void appendRgbMarker(StringBuilder target, int rgb) {
        // Re-emitted runs must use a protocol that is currently enabled. Prefer the compact
        // TiC PUA form when its compatibility switch is on; otherwise the hex-chat switch is
        // necessarily the source of the RGB state. This keeps either feature independently
        // switchable and avoids inventing an always-active hidden protocol.
        if (tinkersRgbEncoding) {
            target.append((char) (TinkersAntiqueSyntaxProvider.MARKER_START
                            + ((rgb >>> 16) & 0xFF)))
                    .append((char) (TinkersAntiqueSyntaxProvider.MARKER_START
                            + ((rgb >>> 8) & 0xFF)))
                    .append((char) (TinkersAntiqueSyntaxProvider.MARKER_START
                            + (rgb & 0xFF)));
        } else {
            target.append(String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF));
        }
    }

    private int clamp(int boundary) {
        return Math.max(0, Math.min(visibleText.length(), boundary));
    }

    private static TreeMap<Integer, State> drawStates(StructuredText text) {
        TreeMap<Integer, State> states = new TreeMap<>();
        State last = State.EMPTY;
        for (int offset = 0; offset < text.plainText().length();) {
            TextStyle style = text.styleAt(offset);
            Character legacyColor = style.hasColorOverride()
                    ? StructuredTextRuntime.legacyColorCode(style.rgb()) : null;
            last = new State(legacyColor,
                    style.hasColorOverride() && legacyColor == null, style.rgb(),
                    style.obfuscated(), style.bold(), style.strikethrough(),
                    style.underline(), style.italic());
            states.put(offset, last);
            offset += Character.charCount(text.plainText().codePointAt(offset));
        }
        states.putIfAbsent(0, State.EMPTY);
        states.put(text.plainText().length(), last);
        return states;
    }

    public static final class State {
        private static final State EMPTY = new State(null, false, 0,
                false, false, false, false, false);

        private final Character legacyColor;
        private final boolean rgbOverride;
        private final int rgb;
        private final boolean random;
        private final boolean bold;
        private final boolean strike;
        private final boolean underline;
        private final boolean italic;

        private State(Character legacyColor, boolean rgbOverride, int rgb, boolean random,
                      boolean bold, boolean strike, boolean underline, boolean italic) {
            this.legacyColor = legacyColor;
            this.rgbOverride = rgbOverride;
            this.rgb = rgb & 0xFFFFFF;
            this.random = random;
            this.bold = bold;
            this.strike = strike;
            this.underline = underline;
            this.italic = italic;
        }

        public boolean hasRgbOverride() { return rgbOverride; }
        public int rgb() { return rgb; }
        public boolean random() { return random; }
        public boolean bold() { return bold; }
        public boolean strike() { return strike; }
        public boolean underline() { return underline; }
        public boolean italic() { return italic; }

        public String prefix() {
            StringBuilder prefix = new StringBuilder();
            if (legacyColor != null) prefix.append('\u00A7').append(legacyColor.charValue());
            if (random) prefix.append("\u00A7k");
            if (bold) prefix.append("\u00A7l");
            if (strike) prefix.append("\u00A7m");
            if (underline) prefix.append("\u00A7n");
            if (italic) prefix.append("\u00A7o");
            return prefix.toString();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof State)) return false;
            State state = (State) other;
            return rgbOverride == state.rgbOverride && rgb == state.rgb && random == state.random
                    && bold == state.bold && strike == state.strike
                    && underline == state.underline && italic == state.italic
                    && Objects.equals(legacyColor, state.legacyColor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(legacyColor, rgbOverride, rgb, random, bold, strike, underline, italic);
        }
    }

}
