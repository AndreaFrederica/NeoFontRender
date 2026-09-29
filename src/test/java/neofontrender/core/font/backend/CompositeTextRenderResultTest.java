package neofontrender.core.font.backend;

import neofontrender.api.text.TextVisualBounds;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CompositeTextRenderResultTest {
    @Test void punctuationAndSpacesKeepAdvanceWithoutExpandingVisibleBounds() {
        RecordingResult word = new RecordingResult(20, new TextVisualBounds(2, 3, 19, 11));
        RecordingResult punctuation = new RecordingResult(10, new TextVisualBounds(1, 8, 4, 11));
        RecordingResult spaces = new RecordingResult(12, TextVisualBounds.EMPTY);
        TextRenderResult result = CompositeTextRenderResult.of(Arrays.asList(word, punctuation, spaces));
        assertEquals(42, result.advance());
        assertEquals(2, result.visualLeft());
        assertEquals(24, result.visualRight());
        assertEquals(3, result.visualTop());
        assertEquals(11, result.visualBottom());
        result.draw(100, 50, 1);
        assertArrayEquals(new float[]{100, 50}, word.origins.get(0));
        assertArrayEquals(new float[]{120, 50}, punctuation.origins.get(0));
        assertArrayEquals(new float[]{130, 50}, spaces.origins.get(0));
    }

    @Test void negativeBearingsAndEmptyPiecesDoNotIntroduceAnArtificialOrigin() {
        TextRenderResult result = CompositeTextRenderResult.of(Arrays.asList(
                new RecordingResult(4, TextVisualBounds.EMPTY),
                new RecordingResult(10, new TextVisualBounds(-2, -3, 11, 8))));
        assertEquals(14, result.advance());
        assertEquals(2, result.visualLeft());
        assertEquals(15, result.visualRight());
        assertEquals(-3, result.visualTop());
        assertTrue(CompositeTextRenderResult.of(Arrays.asList(
                new RecordingResult(4, TextVisualBounds.EMPTY))).visualBounds().isEmpty());
    }

    private static final class RecordingResult implements TextRenderResult {
        final float advance;
        final TextVisualBounds bounds;
        final List<float[]> origins = new ArrayList<>();
        RecordingResult(float advance, TextVisualBounds bounds) {
            this.advance = advance; this.bounds = bounds;
        }
        @Override public float advance() { return advance; }
        @Override public float visualLeft() { return bounds.left; }
        @Override public float visualTop() { return bounds.top; }
        @Override public float visualRight() { return bounds.right; }
        @Override public float visualBottom() { return bounds.bottom; }
        @Override public void draw(float x, float y, float alpha) { origins.add(new float[]{x, y}); }
    }
}
