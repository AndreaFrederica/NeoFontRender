package neofontrender.core.font.backend;

/**
 * Draw-ready shaped text result produced by a {@link TextRenderBackend}.
 */
public interface TextRenderResult {

    TextRenderResult EMPTY = new TextRenderResult() {
        @Override
        public float advance() {
            return 0.0F;
        }

        @Override
        public void draw(float x, float y, float alpha) {
        }
    };

    float advance();

    /** Pixel-space bounds relative to the draw origin; advance alone excludes glyph overhang. */
    default float visualLeft() {
        return 0.0F;
    }

    default float visualRight() {
        return advance();
    }

    default float visualTop() {
        return 0.0F;
    }

    default float visualBottom() {
        return 8.0F;
    }

    void draw(float x, float y, float alpha);
}
