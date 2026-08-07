package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatOutgoingMessageTest {
    @Test
    void recognizesNormalAndPrivateMessages() {
        ChatOutgoingMessage normal = ChatOutgoingMessage.parse("hello @Alice", "/msg {player}", 100L);
        assertNotNull(normal);
        assertFalse(normal.privateMessage);
        assertTrue(normal.matches("<Bob> hello @Alice", 200L));

        ChatOutgoingMessage privateMessage = ChatOutgoingMessage.parse(
                "/msg Bob personal note", "/msg {player}", 100L);
        assertNotNull(privateMessage);
        assertTrue(privateMessage.privateMessage);
        assertTrue(privateMessage.target.equals("Bob"));
        assertTrue(privateMessage.matches("To Bob: personal note", 200L));
    }

    @Test
    void supportsConfiguredPrivateCommandAndExpiresEchoes() {
        ChatOutgoingMessage message = ChatOutgoingMessage.parse(
                "/pm Me saved text", "/pm {player}", 100L);
        assertNotNull(message);
        assertTrue(message.privateMessage);
        assertFalse(message.matches("saved text", 9_000L));
        assertNull(ChatOutgoingMessage.parse("/spawn", "/pm {player}", 100L));
    }
}
