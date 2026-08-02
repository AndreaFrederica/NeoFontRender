package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

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
}
