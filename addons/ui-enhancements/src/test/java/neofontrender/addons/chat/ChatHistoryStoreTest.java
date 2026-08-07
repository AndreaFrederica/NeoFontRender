package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatHistoryStoreTest {

    private static ChatMessageMetadata metadata(long timestamp, ChatSource source, String player) {
        return new ChatMessageMetadata(timestamp, source, player, null);
    }

    @Test
    void roundTripsReceivedMessagesInOrder() throws Exception {
        try (ChatHistoryStore store = ChatHistoryStore.inMemory()) {
            UUID sender = UUID.randomUUID();
            ChatMessageMetadata first = new ChatMessageMetadata(1000, ChatSource.PLAYER, "Alice", sender);
            ChatMessageMetadata second = metadata(2000, ChatSource.SERVER, "");
            store.insertReceived("server:test", 0, first.timestamp, first, "{\"text\":\"a\"}");
            store.insertReceived("server:test", 0, second.timestamp, second, "{\"text\":\"b\"}");
            List<ChatHistoryStore.ReceivedMessage> messages = store.loadReceived("server:test");
            assertEquals(2, messages.size());
            assertEquals("{\"text\":\"a\"}", messages.get(0).json);
            assertEquals("{\"text\":\"b\"}", messages.get(1).json);
            assertEquals(ChatSource.PLAYER, messages.get(0).metadata.source);
            assertEquals("Alice", messages.get(0).metadata.playerName);
            assertEquals(sender, messages.get(0).metadata.playerId);
            assertEquals(second.timestamp, messages.get(1).metadata.timestamp);
        }
    }

    @Test
    void deletingByIdKeepsNewestVariant() throws Exception {
        try (ChatHistoryStore store = ChatHistoryStore.inMemory()) {
            store.insertReceived("server:test", 42, 1000, metadata(1000, ChatSource.SERVER, ""), "old");
            store.deleteReceivedById("server:test", 42);
            store.insertReceived("server:test", 42, 2000, metadata(2000, ChatSource.SERVER, ""), "new");
            List<ChatHistoryStore.ReceivedMessage> messages = store.loadReceived("server:test");
            assertEquals(1, messages.size());
            assertEquals("new", messages.get(0).json);
            assertEquals(42, messages.get(0).msgId);
        }
    }

    @Test
    void deletingByIdIsScoped() throws Exception {
        try (ChatHistoryStore store = ChatHistoryStore.inMemory()) {
            store.insertReceived("server:a", 7, 1000, metadata(1000, ChatSource.SERVER, ""), "a");
            store.insertReceived("server:b", 7, 1000, metadata(1000, ChatSource.SERVER, ""), "b");
            store.deleteReceivedById("server:a", 7);
            assertEquals(0, store.loadReceived("server:a").size());
            assertEquals(1, store.loadReceived("server:b").size());
        }
    }

    @Test
    void trimsReceivedAndSentPerScope() throws Exception {
        try (ChatHistoryStore store = ChatHistoryStore.inMemory()) {
            for (int i = 1; i <= 10; i++) {
                store.insertReceived("server:test", 0, i, metadata(i, ChatSource.SERVER, ""), "m" + i);
                store.insertSent("server:test", "s" + i);
            }
            store.trimReceived("server:test", 3);
            store.trimSent("server:test", 3);
            List<ChatHistoryStore.ReceivedMessage> messages = store.loadReceived("server:test");
            assertEquals(3, messages.size());
            assertEquals("m8", messages.get(0).json);
            assertEquals("m10", messages.get(2).json);
            List<String> sent = store.loadSent("server:test");
            assertEquals(3, sent.size());
            assertEquals("s8", sent.get(0));
            assertEquals("s10", sent.get(2));
        }
    }

    @Test
    void scopesAreIsolated() throws Exception {
        try (ChatHistoryStore store = ChatHistoryStore.inMemory()) {
            store.insertReceived("server:a", 0, 1, metadata(1, ChatSource.SERVER, ""), "a");
            store.insertReceived("server:b", 0, 1, metadata(1, ChatSource.PLAYER, "Bob"), "b");
            store.insertSent("server:b", "hi");
            assertEquals(1, store.loadReceived("server:a").size());
            assertEquals("Bob", store.loadReceived("server:b").get(0).metadata.playerName);
            assertEquals(2, store.scopes().size());
            store.deleteScope("server:a");
            assertEquals(1, store.scopes().size());
            assertEquals(0, store.loadReceived("server:a").size());
        }
    }

    @Test
    void emptyUntilSomethingIsStored() throws Exception {
        try (ChatHistoryStore store = ChatHistoryStore.inMemory()) {
            assertTrue(store.isEmpty());
            store.insertSent("server:a", "x");
            assertFalse(store.isEmpty());
        }
    }

    @Test
    void roundTripsPrivateMetadata() throws Exception {
        try (ChatHistoryStore store = ChatHistoryStore.inMemory()) {
            UUID sender = UUID.randomUUID();
            ChatMessageMetadata metadata = new ChatMessageMetadata(5000, ChatSource.PRIVATE, "Carol",
                    sender, "peer", true, "body text");
            store.insertReceived("server:test", 7, metadata.timestamp, metadata, "{\"private\":true}");
            ChatHistoryStore.ReceivedMessage message = store.loadReceived("server:test").get(0);
            assertEquals(ChatSource.PRIVATE, message.metadata.source);
            assertEquals("Carol", message.metadata.playerName);
            assertEquals(sender, message.metadata.playerId);
            assertEquals("peer", message.metadata.privatePeer);
            assertTrue(message.metadata.outgoing);
            assertEquals("body text", message.metadata.privateBody);
        }
    }
}
