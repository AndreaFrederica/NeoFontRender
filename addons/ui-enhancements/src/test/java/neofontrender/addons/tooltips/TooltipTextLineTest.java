package neofontrender.addons.tooltips;

import neofontrender.api.text.TextVisualBounds;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TooltipTextLineTest {
    @Test void alignsVisibleGlyphsIncludingPositiveBearingAndTrailingPunctuationSpace() {
        // A run can advance 30px while its final punctuation ends at 24px.
        TextVisualBounds visible = new TextVisualBounds(2, 3, 24, 11);
        for (float scale : new float[]{0.5F, 1, 1.25F, 2}) {
            TextVisualBounds bounds = visible.scale(scale);
            float left = TooltipTextLine.alignedOrigin(100, bounds, "left");
            float center = TooltipTextLine.alignedOrigin(100, bounds, "center");
            float right = TooltipTextLine.alignedOrigin(100, bounds, "right");
            assertEquals(0, left + bounds.left, 0.0001F);
            assertEquals(50, center + (bounds.left + bounds.right) / 2, 0.0001F);
            assertEquals(100, right + bounds.right, 0.0001F);
        }
    }

    @Test void positionedRunsAndShadowHaveOneUnionBeforeAlignment() {
        TextVisualBounds first = new TextVisualBounds(-2, 3, 18, 11);
        TextVisualBounds second = new TextVisualBounds(1, 5, 4, 9).translate(20, 2);
        TextVisualBounds foreground = first.union(second);
        TextVisualBounds shadowed = foreground.union(foreground.translate(2, 3));
        assertEquals(-2, shadowed.left);
        assertEquals(26, shadowed.right);
        assertEquals(3, shadowed.top);
        assertEquals(14, shadowed.bottom);
        float x = TooltipTextLine.alignedOrigin(29, shadowed, "center");
        assertEquals(0.5F, x + shadowed.left);
        assertEquals(28.5F, x + shadowed.right);
    }
}
