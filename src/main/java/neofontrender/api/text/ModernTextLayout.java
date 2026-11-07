package neofontrender.api.text;

import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.text.animation.TextAnimationFrame;

import java.util.function.Supplier;

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
    private final Supplier<TextRenderResult> dynamicResult;

    ModernTextLayout(TextRenderResult result, float alpha) {
        this(result, alpha, null);
    }

    private ModernTextLayout(TextRenderResult result, float alpha,
                             Supplier<TextRenderResult> dynamicResult) {
        this.result = result == null ? TextRenderResult.EMPTY : result;
        this.alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        this.dynamicResult = dynamicResult;
    }

    /** Builds a layout around a post-processed draw result. */
    public static ModernTextLayout fromPostProcessResult(TextRenderResult result, float alpha) {
        return new ModernTextLayout(result, alpha);
    }

    /**
     * Creates a layout whose geometry is retained but whose animated draw result is rebuilt at
     * draw time. This keeps retained public API layouts in sync with time-based effects.
     */
    public static ModernTextLayout withDynamicResult(ModernTextLayout initial,
                                                     Supplier<TextRenderResult> supplier) {
        if (initial == null || supplier == null) return initial;
        return new ModernTextLayout(initial.result, initial.alpha, supplier);
    }

    /** Internal bridge for post-processors that need to wrap the draw result. */
    public TextRenderResult resultForPostProcess() {
        return result;
    }

    /** Preserves the caller alpha when a post-processor replaces the result wrapper. */
    public float alphaForPostProcess() {
        return alpha;
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
        try (TextAnimationFrame.Scope ignored = TextAnimationFrame.openCurrent()) {
            TextRenderResult current = dynamicResult == null ? result : dynamicResult.get();
            if (current != null) current.draw(x, y, alpha);
        }
    }

    public void draw(float x, float y, float opacity) {
        try (TextAnimationFrame.Scope ignored = TextAnimationFrame.openCurrent()) {
            TextRenderResult current = dynamicResult == null ? result : dynamicResult.get();
            if (current != null) {
                current.draw(x, y, alpha * Math.max(0.0F, Math.min(1.0F, opacity)));
            }
        }
    }
}
