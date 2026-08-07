package neofontrender.core.font.preprocess;

import neofontrender.api.text.ModernText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private LayoutText(String rawText, String visibleText, int[] rawStarts, int[] rawEnds,
                       TreeMap<Integer, State> states, boolean transformed) {
        this.rawText = rawText;
        this.visibleText = visibleText;
        this.rawStarts = rawStarts;
        this.rawEnds = rawEnds;
        this.states = states;
        this.transformed = transformed;
    }

    public static LayoutText process(String rawText) {
        String raw = rawText == null ? "" : rawText;
        return fromPreprocessed(TextPreprocessingPipeline.process(raw));
    }

    static LayoutText fromPreprocessed(PreprocessedText preprocessed) {
        Objects.requireNonNull(preprocessed, "preprocessed");
        BoundaryText boundaries = stripFormatting(preprocessed);
        return new LayoutText(preprocessed.rawText(), boundaries.visible,
                boundaries.rawStarts, boundaries.rawEnds,
                drawStates(preprocessed.modernText(), boundaries.visible.length()),
                preprocessed.transformed());
    }

    public String rawText() { return rawText; }
    public String visibleText() { return visibleText; }
    public boolean transformed() { return transformed; }

    /** Stable cache discriminator for layout-visible text, source boundaries and draw states. */
    public int fingerprint() {
        int result = Objects.hash(visibleText, states, transformed);
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
        // hex-chat switch is necessarily the source of the RGB state. This avoids inventing an
        // always-active hidden protocol.
        target.append(String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF));
    }

    private int clamp(int boundary) {
        return Math.max(0, Math.min(visibleText.length(), boundary));
    }

    private static BoundaryText stripFormatting(PreprocessedText text) {
        String source = text.visibleText();
        StringBuilder visible = new StringBuilder(source.length());
        List<Integer> starts = new ArrayList<>(source.length() + 1);
        List<Integer> ends = new ArrayList<>(source.length() + 1);
        starts.add(text.rawStartForVisibleBoundary(0));
        ends.add(text.rawEndForVisibleBoundary(0));

        for (int offset = 0; offset < source.length();) {
            if (source.charAt(offset) == '\u00A7' && offset + 1 < source.length()) {
                offset += 2;
                ends.set(ends.size() - 1, text.rawEndForVisibleBoundary(offset));
                continue;
            }
            int codePoint = source.codePointAt(offset);
            int count = Character.charCount(codePoint);
            visible.appendCodePoint(codePoint);
            for (int unit = 1; unit <= count; unit++) {
                int boundary = offset + unit;
                starts.add(text.rawStartForVisibleBoundary(boundary));
                ends.add(text.rawEndForVisibleBoundary(boundary));
            }
            offset += count;
        }
        return new BoundaryText(visible.toString(), toArray(starts), toArray(ends));
    }

    private static TreeMap<Integer, State> drawStates(ModernText modernText, int visibleLength) {
        TreeMap<Integer, State> states = new TreeMap<>();
        int visibleOffset = 0;
        State last = State.EMPTY;
        for (ModernText.Run run : modernText.runs()) {
            MutableLegacyState legacy = new MutableLegacyState();
            String text = run.text();
            for (int offset = 0; offset < text.length();) {
                if (text.charAt(offset) == '\u00A7' && offset + 1 < text.length()) {
                    legacy.apply(text.charAt(offset + 1));
                    offset += 2;
                    continue;
                }
                int codePoint = text.codePointAt(offset);
                int count = Character.charCount(codePoint);
                last = legacy.freeze(run.hasColorOverride(), run.rgb());
                if (visibleOffset < visibleLength) states.put(visibleOffset, last);
                visibleOffset += count;
                offset += count;
            }
        }
        states.putIfAbsent(0, State.EMPTY);
        states.put(visibleLength, last);
        return states;
    }

    private static int[] toArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) result[i] = values.get(i);
        return result;
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

    private static final class MutableLegacyState {
        private Character color;
        private boolean random;
        private boolean bold;
        private boolean strike;
        private boolean underline;
        private boolean italic;

        private void apply(char rawCode) {
            char code = Character.toLowerCase(rawCode);
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                color = code;
                random = bold = strike = underline = italic = false;
                return;
            }
            switch (code) {
                case 'k': random = true; break;
                case 'l': bold = true; break;
                case 'm': strike = true; break;
                case 'n': underline = true; break;
                case 'o': italic = true; break;
                case 'r':
                    color = null;
                    random = bold = strike = underline = italic = false;
                    break;
                default:
                    break;
            }
        }

        private State freeze(boolean rgbOverride, int rgb) {
            return new State(color, rgbOverride, rgb, random, bold, strike, underline, italic);
        }
    }

    private static final class BoundaryText {
        private final String visible;
        private final int[] rawStarts;
        private final int[] rawEnds;

        private BoundaryText(String visible, int[] rawStarts, int[] rawEnds) {
            this.visible = visible;
            this.rawStarts = rawStarts;
            this.rawEnds = rawEnds;
        }
    }
}
