package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ChatHeadGroupingTest {
    @Test void serverMessagesGroupEvenWhenTheyMentionDifferentPlayers() {
        ChatMessageMetadata server = new ChatMessageMetadata(1, ChatSource.SERVER, "", null);
        assertTrue(ChatHeadGrouping.sameSender(UUID.randomUUID(), server, UUID.randomUUID(), server));
    }
    @Test void playerIdentityAndServerBoundariesBreakGroups() {
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        ChatMessageMetadata player = new ChatMessageMetadata(1, ChatSource.PLAYER, "Alice", alice);
        ChatMessageMetadata server = new ChatMessageMetadata(2, ChatSource.SERVER, "", null);
        assertTrue(ChatHeadGrouping.sameSender(alice, player, alice, player));
        assertFalse(ChatHeadGrouping.sameSender(alice, player, bob, player));
        assertFalse(ChatHeadGrouping.sameSender(alice, player, null, server));
        assertFalse(ChatHeadGrouping.sameSender(null, server, alice, player));
    }
    @Test void unresolvedPlayersAreNotServersAndNeedKnownMatchingNames() {
        ChatMessageMetadata alice = new ChatMessageMetadata(1, ChatSource.PLAYER, "Alice", null);
        ChatMessageMetadata unknown = new ChatMessageMetadata(1, ChatSource.PLAYER, "", null);
        assertTrue(ChatHeadGrouping.sameSender(null, alice, null, alice));
        assertFalse(ChatHeadGrouping.sameSender(null, unknown, null, unknown));
        assertFalse(ChatHeadGrouping.sameSender(null, alice, null, null));
    }
}
