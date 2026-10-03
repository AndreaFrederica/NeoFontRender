package neofontrender.text.edit;

import neofontrender.text.InlineSpan;
import neofontrender.text.StructuredEffectSpan;
import neofontrender.text.StructuredText;
import neofontrender.text.UnresolvedSyntax;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

/** Projects every recognized structured-text construct back onto source coordinates. */
public final class SourceEditProjection {
    private final List<SourceSpan> spans;

    private SourceEditProjection(List<SourceSpan> spans) {
        this.spans = Collections.unmodifiableList(spans);
    }

    /** Empty projection used when no enabled structured provider recognized the source. */
    public static SourceEditProjection empty() {
        return new SourceEditProjection(Collections.emptyList());
    }

    public static SourceEditProjection of(StructuredText text) {
        if (text == null) return new SourceEditProjection(Collections.emptyList());
        List<SourceSpan> result = new ArrayList<>();
        for (InlineSpan span : text.inlineSpans()) {
            add(result, text, span.start(), span.end(), span.content().kind(), span.content());
        }
        for (StructuredEffectSpan span : text.effects()) {
            add(result, text, span.start(), span.end(), span.effectId(), null);
        }
        // Unresolved syntax is only editable when a structured provider is
        // active for this text. Otherwise a disabled provider's source must be
        // treated as ordinary literal text, with no source-expansion affordance.
        if (!text.appliedSyntaxProviderIds().isEmpty()) {
            for (UnresolvedSyntax syntax : text.unresolvedSyntax()) {
                if (syntax.sourceEnd() > syntax.sourceStart()) {
                    result.add(new SourceSpan(syntax.sourceStart(), syntax.sourceEnd(),
                            "unresolved", null));
                }
            }
        }
        // Formatting middleware may remove protocol markers from visible text.
        // SourceMap intentionally associates those markers with the next glyph
        // for layout, so it is not a valid edit span by itself. Recognize the
        // built-in source protocols directly and keep their exact boundaries.
        addProtocolMarkers(result, text.sourceText());
        result.sort(Comparator.comparingInt(SourceSpan::start).thenComparingInt(SourceSpan::end));
        return new SourceEditProjection(result);
    }

    private static void addProtocolMarkers(List<SourceSpan> result, String source) {
        for (int index = 0; index < source.length(); index++) {
            if (source.charAt(index) == '#') {
                int end = index + 1;
                while (end + 6 <= source.length() && isHex(source, end, 6)) {
                    end += 6;
                    if (end >= source.length() || source.charAt(end) != '-') break;
                    end++;
                }
                if (end - index >= 7) {
                    result.add(new SourceSpan(index, end, "hex-color", null));
                    index = end - 1;
                }
            } else if (source.charAt(index) >= '\uE700'
                    && source.charAt(index) <= '\uE7FF') {
                int end = index;
                while (end < source.length() && source.charAt(end) >= '\uE700'
                        && source.charAt(end) <= '\uE7FF') end++;
                if (end - index >= 3) {
                    int complete = index + ((end - index) / 3) * 3;
                    result.add(new SourceSpan(index, complete, "tinkers-rgb", null));
                    index = complete - 1;
                }
            }
        }
    }

    private static boolean isHex(String value, int start, int length) {
        if (start < 0 || start + length > value.length()) return false;
        for (int index = start; index < start + length; index++) {
            if (Character.digit(value.charAt(index), 16) < 0) return false;
        }
        return true;
    }

    public List<SourceSpan> spans() { return spans; }

    public SourceSpan at(SourceEditState state) {
        if (state == null) return null;
        for (SourceSpan span : spans) if (span.contains(state)) return span;
        return null;
    }

    public SourcePreviewMode mode(SourceEditState state) {
        return at(state) == null ? SourcePreviewMode.PREVIEW : SourcePreviewMode.RAW;
    }

    private static void add(List<SourceSpan> result, StructuredText text, int plainStart,
                            int plainEnd, String kind, neofontrender.text.InlineContent preview) {
        int start = text.sourceMap().sourceStart(plainStart);
        int end = text.sourceMap().sourceEnd(plainEnd);
        if (end > start) result.add(new SourceSpan(start, end, kind, preview));
    }
}
