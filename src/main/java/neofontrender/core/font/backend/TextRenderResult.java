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

    void draw(float x, float y, float alpha);
}