package neofontrender.addons.server;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerChatHistoryStoreTest {

    @Test
    void roundTripsMessagesInArrivalOrder() throws Exception {
        try (ServerChatHistoryStore store = ServerChatHistoryStore.inMemory()) {
            store.insert("CHAT", ServerChatHistoryStore.SCOPE_GLOBAL, "Alice", "", 1000, "hello");
            store.insert("CHAT", ServerChatHistoryStore.SCOPE_GLOBAL, "Bob", "", 2000, "hi");
            List<ServerChatHistoryStore.Message> messages = store.load(ServerChatHistoryStore.SCOPE_GLOBAL);
            assertEquals(2, messages.size());
            assertEquals("Alice", messages.get(0).sender);
            assertEquals("hello", messages.get(0).message);
            assertEquals(1000, messages.get(0).timestamp);
            assertEquals("CHAT", messages.get(0).type);
            assertEquals("Bob", messages.get(1).sender);
        }
    }

    @Test
    void scopesAreIsolated() throws Exception {
        try (ServerChatHistoryStore store = ServerChatHistoryStore.inMemory()) {
            store.insert("GROUP", ServerChatHistoryStore.GROUP_PREFIX + "team", "Alice", "Bob,Carol", 1000, "go");
            store.insert("PRIVATE", ServerChatHistoryStore.SCOPE_PRIVATE, "Alice", "Bob", 2000, "psst");
            store.insert("CHAT", ServerChatHistoryStore.SCOPE_GLOBAL, "Alice", "", 3000, "public");
            assertEquals(1, store.load(ServerChatHistoryStore.GROUP_PREFIX + "team").size());
            assertEquals("Bob", store.load(ServerChatHistoryStore.SCOPE_PRIVATE).get(0).recipients);
            assertEquals(3, store.scopes().size());
        }
    }

    @Test
    void loadSinceReturnsOnlyNewerMessages() throws Exception {
        try (ServerChatHistoryStore store = ServerChatHistoryStore.inMemory()) {
            for (int i = 1; i <= 5; i++) {
                store.insert("CHAT", ServerChatHistoryStore.SCOPE_GLOBAL, "Alice", "", i, "m" + i);
            }
            List<ServerChatHistoryStore.Message> messages = store.loadSince(ServerChatHistoryStore.SCOPE_GLOBAL, 2, 2);
            assertEquals(2, messages.size());
            assertEquals("m3", messages.get(0).message);
            assertEquals("m4", messages.get(1).message);
        }
    }

    @Test
    void trimsToNewestPerScope() throws Exception {
        try (ServerChatHistoryStore store = ServerChatHistoryStore.inMemory()) {
            for (int i = 1; i <= 10; i++) {
                store.insert("GROUP", ServerChatHistoryStore.GROUP_PREFIX + "team", "Alice", "", i, "m" + i);
            }
            store.trim(ServerChatHistoryStore.GROUP_PREFIX + "team", 3);
            List<ServerChatHistoryStore.Message> messages = store.load(ServerChatHistoryStore.GROUP_PREFIX + "team");
            assertEquals(3, messages.size());
            assertEquals("m8", messages.get(0).message);
            assertEquals("m10", messages.get(2).message);
        }
    }

    @Test
    void emptyUntilSomethingIsStored() throws Exception {
        try (ServerChatHistoryStore store = ServerChatHistoryStore.inMemory()) {
            assertTrue(store.isEmpty());
            store.insert("CHAT", ServerChatHistoryStore.SCOPE_GLOBAL, "Alice", "", 1, "x");
            assertFalse(store.isEmpty());
        }
    }
}
