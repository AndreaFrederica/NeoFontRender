package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ChatLayoutMemoryTest {
    private final ChatLayoutMemory.Viewport large = new ChatLayoutMemory.Viewport(1920, 1080, 2, 1000);
    private final ChatLayoutMemory.Viewport small = new ChatLayoutMemory.Viewport(800, 600, 2, 1000);
    private final ChatLayoutMemory.Bounds original = new ChatLayoutMemory.Bounds(400, 250, 400, 200);
    private final ChatLayoutMemory.Bounds clamped = new ChatLayoutMemory.Bounds(0, 75, 400, 200);

    @Test void roundTripPreservesLargeLayoutAfterSmallScreenClampsIt() {
        var memory = new ChatLayoutMemory();
        memory.enter(large);
        memory.rememberNow(original);
        assertNull(memory.enter(small));
        memory.rememberNow(clamped);
        assertEquals(original, memory.enter(large));
        assertEquals(clamped, memory.enter(small));
        var restarted = new ChatLayoutMemory();
        restarted.replace(ChatLayoutMemory.decode(ChatLayoutMemory.encode(memory.snapshot())));
        assertEquals(original, restarted.enter(large));
    }

    @Test void resizingOnlyRecordsSettledViewportAndBounds() {
        var memory = new ChatLayoutMemory();
        memory.enter(large);
        assertFalse(memory.observe(original, 0, false));
        memory.enter(small);
        assertFalse(memory.observe(clamped, 100, false));
        assertTrue(memory.observe(clamped, ChatLayoutMemory.SETTLE_NANOS + 100, false));
        assertEquals(Map.of(small, clamped), memory.snapshot());
        assertFalse(memory.observe(clamped, ChatLayoutMemory.SETTLE_NANOS * 2, false));
    }

    @Test void dragNeverWritesIntermediateBoundsAndReleaseCommitsImmediately() {
        var memory = new ChatLayoutMemory();
        memory.enter(large);
        assertFalse(memory.observe(original, 0, true));
        assertFalse(memory.observe(clamped, ChatLayoutMemory.SETTLE_NANOS * 2, true));
        assertTrue(memory.snapshot().isEmpty());
        assertTrue(memory.rememberNow(clamped));
        assertEquals(clamped, memory.snapshot().get(large));
    }

    @Test void deletingCurrentProfileStaysDeletedUntilItMoves() {
        var memory = new ChatLayoutMemory();
        memory.enter(large);
        memory.rememberNow(original);
        memory.replace(Map.of());
        assertFalse(memory.observe(original, ChatLayoutMemory.SETTLE_NANOS * 3, false));
        assertTrue(memory.snapshot().isEmpty());
        assertFalse(memory.observe(clamped, ChatLayoutMemory.SETTLE_NANOS * 4, false));
        assertTrue(memory.observe(clamped, ChatLayoutMemory.SETTLE_NANOS * 5, false));
    }

    @Test void guiAndChatScalesHaveIndependentProfiles() {
        var memory = new ChatLayoutMemory();
        memory.enter(large);
        memory.rememberNow(original);
        assertNull(memory.enter(new ChatLayoutMemory.Viewport(1920, 1080, 3, 1000)));
        assertNull(memory.enter(new ChatLayoutMemory.Viewport(1920, 1080, 2, 500)));
        assertEquals(original, memory.enter(large));
    }

    @Test void damagedEntriesDoNotDiscardValidOnes() {
        var rows = new java.util.ArrayList<>(ChatLayoutMemory.encode(Map.of(large, original)));
        rows.addAll(List.of("broken", "0:1080:2:1000:0:0:300:100", "800:600:2:1000:0:0:-1:100"));
        assertEquals(Map.of(large, original), ChatLayoutMemory.decode(rows));
    }
}
