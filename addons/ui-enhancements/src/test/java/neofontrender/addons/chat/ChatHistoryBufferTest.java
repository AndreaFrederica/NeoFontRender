package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatHistoryBufferTest {
    @Test
    void replacesMatchingNonZeroIdsAndRetainsOrder() {
        ChatHistoryBuffer buffer = new ChatHistoryBuffer();
        buffer.recordReceived(7, "old", 10);
        buffer.recordReceived(0, "ordinary", 10);
        buffer.recordReceived(7, "new", 10);

        ChatHistoryData snapshot = buffer.snapshot(true, true);
        assertEquals(Arrays.asList(
                new ChatHistoryEntry(0, "ordinary"),
                new ChatHistoryEntry(7, "new")), snapshot.received());
    }

    @Test
    void suppressesAdjacentSentDuplicatesAndKeepsNewestEntries() {
        ChatHistoryBuffer buffer = new ChatHistoryBuffer();
        buffer.recordSent("one", 2);
        buffer.recordSent("one", 2);
        buffer.recordSent("two", 2);
        buffer.recordSent("three", 2);

        assertEquals(Arrays.asList("two", "three"), buffer.snapshot(true, true).sent());
    }

    @Test
    void replacementSnapshotIsTrimmedToConfiguredLimit() {
        ChatHistoryBuffer buffer = new ChatHistoryBuffer();
        buffer.replace(new ChatHistoryData(
                Arrays.asList(
                        new ChatHistoryEntry(0, "one"),
                        new ChatHistoryEntry(0, "two"),
                        new ChatHistoryEntry(0, "three")),
                Arrays.asList("one", "two", "three")), 2);

        ChatHistoryData snapshot = buffer.snapshot(true, true);
        assertEquals(Arrays.asList(
                new ChatHistoryEntry(0, "two"),
                new ChatHistoryEntry(0, "three")), snapshot.received());
        assertEquals(Arrays.asList("two", "three"), snapshot.sent());
    }
}
