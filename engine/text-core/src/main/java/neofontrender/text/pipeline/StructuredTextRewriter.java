package neofontrender.text.pipeline;

import neofontrender.text.InlineContent;
import neofontrender.text.InlineLayout;
import neofontrender.text.InlineSpan;
import neofontrender.text.SourceMap;
import neofontrender.text.StructuredEffectSpan;
import neofontrender.text.StructuredText;
import neofontrender.text.StyledSpan;
import neofontrender.text.TextStyle;
import neofontrender.text.UnresolvedSyntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/** Applies non-overlapping plain-text replacements while retaining source coordinates. */
public final class StructuredTextRewriter {
    private StructuredTextRewriter() {}

    public static StructuredText rewrite(StructuredText input, List<Replacement> replacements,
                                         String middlewareId) {
        Objects.requireNonNull(input, "input");
        if (replacements == null || replacements.isEmpty()) return input;
        List<Replacement> ordered = new ArrayList<>(replacements);
        ordered.sort(Comparator.comparingInt(Replacement::start));
        validate(input.plainText().length(), ordered);

        StringBuilder plain = new StringBuilder();
        List<TextStyle> charStyles = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        List<InlineSpan> inline = new ArrayList<>();
        int[] oldToNew = new int[input.plainText().length() + 1];
        java.util.Arrays.fill(oldToNew, -1);
        starts.add(input.sourceMap().sourceStart(0));
        ends.add(input.sourceMap().sourceEnd(0));

        int cursor = 0;
        for (Replacement replacement : ordered) {
            appendOriginal(input, cursor, replacement.start, plain, charStyles, starts, ends,
                    oldToNew);
            oldToNew[replacement.start] = plain.length();
            int outputStart = plain.length();
            TextStyle base = input.styleAt(Math.min(replacement.start,
                    Math.max(0, input.plainText().length() - 1)));
            TextStyle style = replacement.style.apply(base);
            int sourceStart = input.sourceMap().sourceStart(replacement.start);
            int sourceEnd = input.sourceMap().sourceEnd(replacement.end);
            if (replacement.text.isEmpty()) {
                ends.set(ends.size() - 1, sourceEnd);
            }
            String output = replacement.text;
            int inlineOffset = 0;
            if (replacement.inline != null
                    && replacement.inline.layout().flow() == InlineLayout.Flow.BLOCK) {
                int leadingBreaks = Math.max(
                        outputStart > 0 && plain.charAt(outputStart - 1) != '\n' ? 1 : 0,
                        Math.max(0, (int) Math.ceil(replacement.inline.layout().rows()) - 1));
                StringBuilder block = new StringBuilder(leadingBreaks + 2);
                for (int index = 0; index < leadingBreaks; index++) block.append('\n');
                inlineOffset = block.length();
                block.append('\uFFFC');
                if (replacement.end < input.plainText().length()
                        && input.plainText().charAt(replacement.end) != '\n') block.append('\n');
                output = block.toString();
            }
            for (int index = 0; index < output.length(); index++) {
                plain.append(output.charAt(index));
                charStyles.add(style);
                starts.add(sourceStart);
                ends.add(sourceEnd);
            }
            if (replacement.inline != null && plain.length() > outputStart + inlineOffset) {
                inline.add(new InlineSpan(outputStart + inlineOffset,
                        outputStart + inlineOffset + 1, replacement.inline));
            }
            for (int boundary = replacement.start + 1; boundary <= replacement.end; boundary++) {
                oldToNew[boundary] = plain.length();
            }
            cursor = replacement.end;
        }
        appendOriginal(input, cursor, input.plainText().length(), plain, charStyles, starts, ends,
                oldToNew);

        for (InlineSpan existing : input.inlineSpans()) {
            int from = mapped(oldToNew, existing.start());
            int to = mapped(oldToNew, existing.end());
            if (to > from) inline.add(new InlineSpan(from, to, existing.content()));
        }
        inline.sort(Comparator.comparingInt(InlineSpan::start));

        List<StructuredEffectSpan> effects = new ArrayList<>();
        for (StructuredEffectSpan effect : input.effects()) {
            int from = mapped(oldToNew, effect.start());
            int to = mapped(oldToNew, effect.end());
            if (to > from) effects.add(new StructuredEffectSpan(from, to, effect.effectId(),
                    effect.parameters(), effect.lineWide()));
        }

        LinkedHashSet<String> applied = new LinkedHashSet<>(input.appliedMiddlewareIds());
        applied.add(middlewareId);
        return new StructuredText(input.sourceText(), plain.toString(), spans(charStyles), effects,
                new ArrayList<UnresolvedSyntax>(input.unresolvedSyntax()),
                new SourceMap(input.sourceText().length(), toArray(starts), toArray(ends)),
                input.appliedSyntaxProviderIds(), inline, new ArrayList<>(applied));
    }

    private static void appendOriginal(StructuredText input, int from, int to, StringBuilder plain,
                                       List<TextStyle> styles, List<Integer> starts,
                                       List<Integer> ends, int[] oldToNew) {
        oldToNew[from] = plain.length();
        for (int index = from; index < to; index++) {
            plain.append(input.plainText().charAt(index));
            styles.add(input.styleAt(index));
            starts.add(input.sourceMap().sourceStart(index + 1));
            ends.add(input.sourceMap().sourceEnd(index + 1));
            oldToNew[index + 1] = plain.length();
        }
    }

    private static List<StyledSpan> spans(List<TextStyle> styles) {
        if (styles.isEmpty()) return Collections.emptyList();
        List<StyledSpan> result = new ArrayList<>();
        int start = 0;
        TextStyle current = styles.get(0);
        for (int index = 1; index <= styles.size(); index++) {
            if (index < styles.size() && current.equals(styles.get(index))) continue;
            result.add(new StyledSpan(start, index, current));
            if (index < styles.size()) {
                start = index;
                current = styles.get(index);
            }
        }
        return result;
    }

    private static int mapped(int[] values, int boundary) {
        int index = Math.max(0, Math.min(values.length - 1, boundary));
        if (values[index] >= 0) return values[index];
        for (int scan = index; scan >= 0; scan--) if (values[scan] >= 0) return values[scan];
        return 0;
    }

    private static int[] toArray(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void validate(int length, List<Replacement> values) {
        int end = 0;
        for (Replacement value : values) {
            if (value.start < end || value.start < 0 || value.end <= value.start
                    || value.end > length) {
                throw new IllegalArgumentException("Overlapping or invalid replacement range");
            }
            end = value.end;
        }
    }

    public static Replacement text(int start, int end, String text,
                                   UnaryOperator<TextStyle> style) {
        return new Replacement(start, end, text, style, null);
    }

    public static Replacement inline(int start, int end, InlineContent content) {
        return new Replacement(start, end, "\uFFFC", UnaryOperator.identity(), content);
    }

    public static final class Replacement {
        private final int start;
        private final int end;
        private final String text;
        private final UnaryOperator<TextStyle> style;
        private final InlineContent inline;

        private Replacement(int start, int end, String text, UnaryOperator<TextStyle> style,
                            InlineContent inline) {
            this.start = start;
            this.end = end;
            this.text = Objects.requireNonNull(text, "text");
            this.style = style == null ? UnaryOperator.identity() : style;
            this.inline = inline;
        }

        public int start() { return start; }
        public int end() { return end; }
    }
}
