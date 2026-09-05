package neofontrender.api.text.route;

import neofontrender.text.StructuredText;

import java.util.List;

/** One selected route's layout contract, shared by drawing and interaction APIs. */
public interface TextRenderRouteLayout {
    String routeId();
    boolean handled();
    String source();
    StructuredText structuredText();
    float advance();
    float height();
    void draw(float x, float y);
    int sourceIndexAt(float localX);
    float widthToSource(int sourceIndex);
    int sourceIndexFitting(float maximumWidth);
    int sourceStartFittingReverse(float maximumWidth);
    int sizeToWidth(float maximumWidth, boolean cjkLineBreak);
    List<TextInlineBounds> inlineBounds();

    default boolean hasInlineContent() {
        return !inlineBounds().isEmpty();
    }

    default boolean hasInlineContentInSourceRange(int start, int end) {
        int from = Math.max(0, Math.min(source().length(), start));
        int to = Math.max(from, Math.min(source().length(), end));
        for (TextInlineBounds bounds : inlineBounds()) {
            if (bounds.sourceStart() < to && bounds.sourceEnd() > from) return true;
        }
        return false;
    }

    default TextInlineBounds contentAt(float localX, float localY) {
        for (TextInlineBounds bounds : inlineBounds()) {
            if (bounds.contains(localX, localY)) return bounds;
        }
        return null;
    }
}
