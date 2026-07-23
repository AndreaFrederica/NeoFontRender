package neofontrender.api.text;

import neofontrender.core.font.backend.TextRenderResult;

/**
 * Draw-ready native-size text produced by {@link ModernTextApi}.
 *
 * <p>Layouts are owned by NFR's renderer caches and must not be closed by callers. Obtain a new
 * layout after a resource reload or font-setting change.</p>
 */
public final class ModernTextLayout {
    static final ModernTextLayout EMPTY = new ModernTextLayout(TextRenderResult.EMPTY, 1.0F);

    private final TextRenderResult result;
    private final float alpha;

    ModernTextLayout(TextRenderResult result, float alpha) {
        this.result = result == null ? TextRenderResult.EMPTY : result;
        this.alpha = Math.max(0.0F, Math.min(1.0F, alpha));
    }

    public float advance() {
        return result.advance();
    }

    public float visualLeft() {
        return result.visualLeft();
    }

    public float visualRight() {
        return result.visualRight();
    }

    public float visualTop() {
        return result.visualTop();
    }

    public float visualBottom() {
        return result.visualBottom();
    }

    public void draw(float x, float y) {
        result.draw(x, y, alpha);
    }

    public void draw(float x, float y, float opacity) {
        result.draw(x, y, alpha * Math.max(0.0F, Math.min(1.0F, opacity)));
    }
}
