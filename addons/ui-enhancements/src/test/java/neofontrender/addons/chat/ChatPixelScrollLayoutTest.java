package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatPixelScrollLayoutTest {
    private static final List<Integer> HEIGHTS = List.of(9, 52, 9, 18);

    @Test
    void resolvesOffsetsAcrossUnevenRows() {
        ChatPixelScrollLayout.Position first = locate(8);
        assertEquals(0, first.index);
        assertEquals(8.0F, first.remainder);

        ChatPixelScrollLayout.Position image = locate(18);
        assertEquals(1, image.index);
        assertEquals(9.0F, image.remainder);

        ChatPixelScrollLayout.Position afterImage = locate(61);
        assertEquals(2, afterImage.index);
        assertEquals(0.0F, afterImage.remainder);
    }

    @Test
    void computesPixelMaximumAndLineOffsets() {
        assertEquals(58.0F, ChatPixelScrollLayout.maximum(HEIGHTS, 30, value -> value));
        assertEquals(61.0F, ChatPixelScrollLayout.offsetForIndex(HEIGHTS, 2, value -> value));
        assertEquals(2, ChatPixelScrollLayout.maximumIndex(HEIGHTS, 30, value -> value));
    }

    @Test
    void cachedIndexMeasuresRowsOnceAndReusesPrefixHeights() {
        AtomicInteger measurements = new AtomicInteger();
        ChatPixelScrollLayout.Index index = ChatPixelScrollLayout.index(HEIGHTS, value -> {
            measurements.incrementAndGet();
            return value;
        });

        assertEquals(HEIGHTS.size(), measurements.get());
        assertEquals(88, index.totalHeight());
        assertEquals(52, index.height(1));
        assertEquals(61, index.offsetForIndex(2));
        assertEquals(58.0F, index.maximum(30));
        assertEquals(1, index.locate(18).index);
        assertEquals(9.0F, index.locate(18).remainder);
        assertEquals(HEIGHTS.size(), measurements.get());
    }

    private static ChatPixelScrollLayout.Position locate(float offset) {
        return ChatPixelScrollLayout.locate(HEIGHTS, offset, value -> value);
    }
}
