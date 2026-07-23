package neofontrender.core.font.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Draw result composed from adjacent backend layouts.
 */
public final class CompositeTextRenderResult implements TextRenderResult {
    private final List<Piece> pieces;
    private final float advance;
    private final float visualLeft;
    private final float visualRight;
    private final float visualTop;
    private final float visualBottom;

    private CompositeTextRenderResult(List<Piece> pieces, float advance,
                                      float visualLeft, float visualRight,
                                      float visualTop, float visualBottom) {
        this.pieces = pieces;
        this.advance = advance;
        this.visualLeft = visualLeft;
        this.visualRight = visualRight;
        this.visualTop = visualTop;
        this.visualBottom = visualBottom;
    }

    public static TextRenderResult of(List<TextRenderResult> results) {
        if (results == null || results.isEmpty()) return TextRenderResult.EMPTY;
        List<Piece> pieces = new ArrayList<>(results.size());
        float cursor = 0.0F;
        float left = 0.0F;
        float right = 0.0F;
        float top = Float.POSITIVE_INFINITY;
        float bottom = Float.NEGATIVE_INFINITY;
        for (TextRenderResult result : results) {
            if (result == null) continue;
            pieces.add(new Piece(result, cursor));
            left = Math.min(left, cursor + result.visualLeft());
            right = Math.max(right, cursor + result.visualRight());
            top = Math.min(top, result.visualTop());
            bottom = Math.max(bottom, result.visualBottom());
            cursor += result.advance();
        }
        if (pieces.isEmpty()) return TextRenderResult.EMPTY;
        if (!Float.isFinite(top)) top = 0.0F;
        if (!Float.isFinite(bottom)) bottom = 0.0F;
        return new CompositeTextRenderResult(
                Collections.unmodifiableList(pieces), cursor, left,
                Math.max(right, cursor), top, bottom);
    }

    @Override
    public float advance() {
        return advance;
    }

    @Override
    public float visualLeft() {
        return visualLeft;
    }

    @Override
    public float visualRight() {
        return visualRight;
    }

    @Override
    public float visualTop() {
        return visualTop;
    }

    @Override
    public float visualBottom() {
        return visualBottom;
    }

    @Override
    public void draw(float x, float y, float alpha) {
        for (Piece piece : pieces) {
            piece.result.draw(x + piece.offset, y, alpha);
        }
    }

    private static final class Piece {
        private final TextRenderResult result;
        private final float offset;

        private Piece(TextRenderResult result, float offset) {
            this.result = result;
            this.offset = offset;
        }
    }
}
