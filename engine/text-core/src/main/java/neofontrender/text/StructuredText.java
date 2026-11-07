package neofontrender.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable output shared by syntax, layout, raster, and post-processing stages. */
public final class StructuredText {
    private final String sourceText;
    private final String plainText;
    private final List<StyledSpan> styles;
    private final List<StructuredEffectSpan> effects;
    private final List<InlineSpan> inlineSpans;
    private final List<UnresolvedSyntax> unresolved;
    private final List<String> appliedSyntaxProviderIds;
    private final List<String> appliedMiddlewareIds;
    private final SourceMap sourceMap;

    public StructuredText(String sourceText, String plainText, List<StyledSpan> styles,
                          List<StructuredEffectSpan> effects,
                          List<UnresolvedSyntax> unresolved, SourceMap sourceMap) {
        this(sourceText, plainText, styles, effects, unresolved, sourceMap,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    public StructuredText(String sourceText, String plainText, List<StyledSpan> styles,
                          List<StructuredEffectSpan> effects,
                          List<UnresolvedSyntax> unresolved, SourceMap sourceMap,
                          List<String> appliedSyntaxProviderIds) {
        this(sourceText, plainText, styles, effects, unresolved, sourceMap,
                appliedSyntaxProviderIds, Collections.emptyList(), Collections.emptyList());
    }

    public StructuredText(String sourceText, String plainText, List<StyledSpan> styles,
                          List<StructuredEffectSpan> effects,
                          List<UnresolvedSyntax> unresolved, SourceMap sourceMap,
                          List<String> appliedSyntaxProviderIds, List<InlineSpan> inlineSpans,
                          List<String> appliedMiddlewareIds) {
        this.sourceText = Objects.requireNonNull(sourceText, "sourceText");
        this.plainText = Objects.requireNonNull(plainText, "plainText");
        this.styles = immutable(styles);
        this.effects = immutable(effects);
        this.inlineSpans = immutable(inlineSpans);
        this.unresolved = immutable(unresolved);
        this.sourceMap = Objects.requireNonNull(sourceMap, "sourceMap");
        this.appliedSyntaxProviderIds = immutable(appliedSyntaxProviderIds);
        this.appliedMiddlewareIds = immutable(appliedMiddlewareIds);
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null || values.isEmpty() ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }

    public String sourceText() { return sourceText; }
    public String plainText() { return plainText; }
    public List<StyledSpan> styles() { return styles; }
    public List<StructuredEffectSpan> effects() { return effects; }
    public List<InlineSpan> inlineSpans() { return inlineSpans; }
    public List<UnresolvedSyntax> unresolvedSyntax() { return unresolved; }
    public List<String> appliedSyntaxProviderIds() { return appliedSyntaxProviderIds; }
    public List<String> appliedMiddlewareIds() { return appliedMiddlewareIds; }
    public SourceMap sourceMap() { return sourceMap; }
    public boolean animated() {
        for (StyledSpan span : styles) if (span.style().obfuscated()) return true;
        for (StructuredEffectSpan span : effects) {
            if (span.animationRenderMode()
                    == neofontrender.text.animation.TextAnimationRenderMode.GLYPH) return true;
        }
        return false;
    }

    public TextStyle styleAt(int index) {
        for (StyledSpan span : styles) {
            if (index >= span.start() && index < span.end()) return span.style();
        }
        return TextStyle.DEFAULT;
    }

    /**
     * Returns a structured slice in plain-text coordinates while preserving the corresponding
     * source controls, styles, effects, unresolved syntax, and source-boundary mapping.
     */
    public StructuredText slice(int plainStart, int plainEnd) {
        int start = Math.max(0, Math.min(plainText.length(), plainStart));
        int end = Math.max(start, Math.min(plainText.length(), plainEnd));
        int sourceStart = sourceMap.sourceStart(start);
        int sourceEnd = sourceMap.sourceEnd(end);
        sourceStart = Math.max(0, Math.min(sourceText.length(), sourceStart));
        sourceEnd = Math.max(sourceStart, Math.min(sourceText.length(), sourceEnd));

        List<StyledSpan> slicedStyles = new ArrayList<>();
        for (StyledSpan span : styles) {
            int from = Math.max(start, span.start());
            int to = Math.min(end, span.end());
            if (to > from) slicedStyles.add(new StyledSpan(from - start, to - start, span.style()));
        }
        List<StructuredEffectSpan> slicedEffects = new ArrayList<>();
        for (StructuredEffectSpan effect : effects) {
            int from = Math.max(start, effect.start());
            int to = Math.min(end, effect.end());
            if (to > from) {
                slicedEffects.add(new StructuredEffectSpan(from - start, to - start,
                        effect.effectId(), effect.parameters(), effect.lineWide(),
                        effect.animationRenderMode()));
            }
        }
        List<InlineSpan> slicedInline = new ArrayList<>();
        for (InlineSpan inline : inlineSpans) {
            int from = Math.max(start, inline.start());
            int to = Math.min(end, inline.end());
            if (to > from) slicedInline.add(new InlineSpan(from - start, to - start, inline.content()));
        }
        List<UnresolvedSyntax> slicedUnresolved = new ArrayList<>();
        for (UnresolvedSyntax value : unresolved) {
            if (value.sourceEnd() <= sourceStart || value.sourceStart() >= sourceEnd) continue;
            slicedUnresolved.add(new UnresolvedSyntax(
                    Math.max(0, value.sourceStart() - sourceStart),
                    Math.min(sourceEnd - sourceStart, value.sourceEnd() - sourceStart),
                    value.source()));
        }
        int length = end - start;
        int[] starts = new int[length + 1];
        int[] ends = new int[length + 1];
        for (int index = 0; index <= length; index++) {
            starts[index] = clampSource(sourceMap.sourceStart(start + index) - sourceStart,
                    sourceEnd - sourceStart);
            ends[index] = clampSource(sourceMap.sourceEnd(start + index) - sourceStart,
                    sourceEnd - sourceStart);
        }
        return new StructuredText(sourceText.substring(sourceStart, sourceEnd),
                plainText.substring(start, end), slicedStyles, slicedEffects, slicedUnresolved,
                new SourceMap(sourceEnd - sourceStart, starts, ends), appliedSyntaxProviderIds,
                slicedInline, appliedMiddlewareIds);
    }

    private static int clampSource(int value, int length) {
        return Math.max(0, Math.min(length, value));
    }
}
