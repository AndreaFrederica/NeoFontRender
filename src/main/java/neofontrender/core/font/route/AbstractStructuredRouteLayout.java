package neofontrender.core.font.route;

import neofontrender.api.text.route.TextInlineBounds;
import neofontrender.api.text.route.TextRenderRouteLayout;
import neofontrender.api.text.route.TextRenderRouteRequest;
import neofontrender.core.font.pipeline.StructuredTextRuntime;
import neofontrender.text.InlineContent;
import neofontrender.text.InlineSpan;
import neofontrender.text.StructuredText;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Shared source mapping, atomic inline bounds, trimming, and fitting implementation. */
abstract class AbstractStructuredRouteLayout implements TextRenderRouteLayout {
    final TextRenderRouteRequest request;
    private final String routeId;
    private final float advance;
    private final float height;
    private volatile List<TextInlineBounds> inlineBounds;

    AbstractStructuredRouteLayout(String routeId, TextRenderRouteRequest request,
                                  float advance, float height) {
        this.routeId = routeId;
        this.request = request;
        this.advance = Math.max(0.0F, advance);
        this.height = Math.max(1.0F, height);
    }

    @Override public final String routeId() { return routeId; }
    @Override public final boolean handled() { return true; }
    @Override public final String source() { return request.source(); }
    @Override public final StructuredText structuredText() { return request.structuredText(); }
    @Override public final float advance() { return advance; }
    @Override public final float height() { return height; }

    abstract float measure(StructuredText text);

    @Override
    public final int sourceIndexAt(float localX) {
        if (localX <= 0.0F) return 0;
        if (localX >= advance) return source().length();
        StructuredText text = structuredText();
        List<Integer> boundaries = graphemeBoundaries(text.plainText());
        float previous = 0.0F;
        for (int index = 1; index < boundaries.size(); index++) {
            int boundary = boundaries.get(index);
            float current = measure(text.slice(0, boundary));
            if (localX < (previous + current) * 0.5F) {
                return text.sourceMap().sourceStart(boundaries.get(index - 1));
            }
            if (localX < current) return text.sourceMap().sourceEnd(boundary);
            previous = current;
        }
        return source().length();
    }

    @Override
    public final float widthToSource(int sourceIndex) {
        StructuredText text = structuredText();
        int sourceBoundary = Math.max(0, Math.min(source().length(), sourceIndex));
        int plainBoundary = 0;
        for (int index = 0; index <= text.plainText().length(); index++) {
            if (text.sourceMap().sourceEnd(index) > sourceBoundary) break;
            plainBoundary = index;
        }
        return measure(text.slice(0, plainBoundary));
    }

    @Override
    public final int sourceIndexFitting(float maximumWidth) {
        StructuredText text = structuredText();
        int accepted = 0;
        for (int boundary : graphemeBoundaries(text.plainText())) {
            if (boundary == 0) continue;
            if (measure(text.slice(0, boundary)) > Math.max(0.0F, maximumWidth)) break;
            accepted = boundary;
        }
        return text.sourceMap().sourceEnd(accepted);
    }

    @Override
    public final int sourceStartFittingReverse(float maximumWidth) {
        StructuredText text = structuredText();
        List<Integer> boundaries = graphemeBoundaries(text.plainText());
        int accepted = text.plainText().length();
        for (int index = boundaries.size() - 2; index >= 0; index--) {
            int boundary = boundaries.get(index);
            if (measure(text.slice(boundary, text.plainText().length()))
                    > Math.max(0.0F, maximumWidth)) break;
            accepted = boundary;
        }
        return text.sourceMap().sourceStart(accepted);
    }

    @Override
    public final int sizeToWidth(float maximumWidth, boolean cjkLineBreak) {
        StructuredText text = structuredText();
        String plain = text.plainText();
        List<Integer> boundaries = graphemeBoundaries(plain);
        Set<Integer> providerBreaks = cjkLineBreak
                ? new HashSet<>(StructuredTextRuntime.breakOpportunities(text))
                : Collections.emptySet();
        int breakPlain = -1;
        for (int index = 1; index < boundaries.size(); index++) {
            int start = boundaries.get(index - 1);
            int end = boundaries.get(index);
            int codePoint = plain.codePointAt(start);
            if (codePoint == '\n') return text.sourceMap().sourceStart(start);
            if (codePoint == ' ' || cjkLineBreak && providerBreaks.contains(start)) {
                breakPlain = start;
            }
            if (measure(text.slice(0, end)) > Math.max(0.0F, maximumWidth)) {
                int chosen = breakPlain >= 0 && breakPlain < end ? breakPlain : end;
                return text.sourceMap().sourceEnd(chosen);
            }
        }
        return source().length();
    }

    @Override
    public final List<TextInlineBounds> inlineBounds() {
        List<TextInlineBounds> current = inlineBounds;
        if (current != null) return current;
        List<TextInlineBounds> result = new ArrayList<>();
        StructuredText text = structuredText();
        for (InlineSpan span : text.inlineSpans()) {
            float x = measure(text.slice(0, span.start()));
            float width = inlineWidth(span.content(), logicalFontSize());
            float objectHeight = inlineHeight(span.content(), logicalFontSize());
            float objectTop = span.content().layout().top(0.0F, 8.0F, objectHeight)
                    + inlineDrawOffset();
            result.add(new TextInlineBounds(text.sourceMap().sourceStart(span.start()),
                    text.sourceMap().sourceEnd(span.end()), x, objectTop,
                    width, objectHeight, span.content()));
        }
        current = Collections.unmodifiableList(result);
        inlineBounds = current;
        return current;
    }

    float logicalFontSize() {
        return request.fontSize();
    }

    float inlineDrawOffset() {
        return 0.0F;
    }

    static InlineGeometry inlineGeometry(TextRenderRouteRequest request, float fontSize,
                                         float normalHeight) {
        float minimumTop = 0.0F;
        float maximumBottom = Math.max(1.0F, normalHeight);
        for (InlineSpan span : request.structuredText().inlineSpans()) {
            float objectHeight = inlineHeight(span.content(), fontSize);
            float objectTop = span.content().layout().top(0.0F, 8.0F, objectHeight);
            minimumTop = Math.min(minimumTop, objectTop);
            maximumBottom = Math.max(maximumBottom, objectTop + objectHeight);
        }
        return new InlineGeometry(maximumBottom - minimumTop, -minimumTop);
    }

    static final class InlineGeometry {
        final float height;
        final float drawOffset;

        InlineGeometry(float height, float drawOffset) {
            this.height = Math.max(1.0F, height);
            this.drawOffset = Math.max(0.0F, drawOffset);
        }
    }

    static float inlineHeight(InlineContent content, float fontSize) {
        return content.layout().resolve(content.raster(), fontSize).height();
    }

    static float inlineWidth(InlineContent content, float fontSize) {
        return content.layout().resolve(content.raster(), fontSize).width();
    }

    private static List<Integer> graphemeBoundaries(String text) {
        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(text);
        List<Integer> result = new ArrayList<>();
        for (int boundary = iterator.first(); boundary != BreakIterator.DONE;
             boundary = iterator.next()) result.add(boundary);
        if (result.isEmpty()) result.add(0);
        return result;
    }
}
