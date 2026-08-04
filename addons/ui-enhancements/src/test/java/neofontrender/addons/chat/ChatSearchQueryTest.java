package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatSearchQueryTest {
    private static final long TIME = LocalDateTime.of(LocalDate.now(), LocalTime.of(13, 30))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    @Test
    void combinesTextSourcePlayerAndTimeFilters() {
        ChatSearchEntry entry = new ChatSearchEntry("Hello from the mine",
                new ChatMessageMetadata(TIME, ChatSource.PLAYER, "Alice", null));
        assertTrue(ChatSearchQuery.parse(
                "hello type:player from:ali after:13:00 before:14:00").matches(entry));
        assertFalse(ChatSearchQuery.parse("type:server").matches(entry));
        assertFalse(ChatSearchQuery.parse("after:14:00").matches(entry));
    }

    @Test
    void supportsChineseAliasesAndRegex() {
        ChatSearchEntry entry = new ChatSearchEntry("交易完成 128 个物品",
                new ChatMessageMetadata(TIME, ChatSource.SERVER, "", null));
        assertTrue(ChatSearchQuery.parse("来源:服务器 /交易.*128/").matches(entry));
        assertFalse(ChatSearchQuery.parse("/[invalid/").matches(entry));
    }

    @Test
    void matchesCaseInsensitivelyAgainstPrecomputedLowerText() {
        ChatSearchEntry entry = new ChatSearchEntry("Hello World",
                new ChatMessageMetadata(TIME, ChatSource.SERVER, "", null));
        assertTrue(ChatSearchQuery.parse("world").matches(entry));
        assertFalse(ChatSearchQuery.parse("xyz").matches(entry));
    }

    @Test
    void highlightsAllTermOccurrences() {
        ChatSearchQuery query = ChatSearchQuery.parse("交易 物品");
        List<int[]> ranges = query.highlightRanges("交易完成 128 个物品交易");
        assertArrayEquals(new int[] {0, 2}, ranges.get(0));
        // 相邻命中（物品/交易）合并为连续高亮区间
        assertArrayEquals(new int[] {10, 14}, ranges.get(1));
    }

    @Test
    void highlightsMergesOverlappingRanges() {
        ChatSearchQuery query = ChatSearchQuery.parse("ab bc");
        List<int[]> ranges = query.highlightRanges("abc");
        assertArrayEquals(new int[] {0, 3}, ranges.get(0));
    }

    @Test
    void highlightsIsCaseInsensitive() {
        ChatSearchQuery query = ChatSearchQuery.parse("mine");
        List<int[]> ranges = query.highlightRanges("deep Mine shaft MINE");
        assertArrayEquals(new int[] {5, 9}, ranges.get(0));
        assertArrayEquals(new int[] {16, 20}, ranges.get(1));
    }
}
