package neofontrender.addons.chat.network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatPolicySessionTest {
    @Test void delayedReplyFromPreviousConnectionCannotGrantPermission() {
        ChatPolicySession session = new ChatPolicySession();
        Object first = new Object();
        Object second = new Object();
        long firstRequest = session.begin(first);
        long secondRequest = session.begin(second);
        session.accept(first, firstRequest, ChatPolicySession.PROTOCOL_VERSION, true);
        assertFalse(session.allowsSectionSign(first));
        session.accept(second, secondRequest, ChatPolicySession.PROTOCOL_VERSION, true);
        assertTrue(session.allowsSectionSign(second));
        session.disconnect(first);
        assertTrue(session.allowsSectionSign(second));
        session.disconnect(second);
        assertFalse(session.allowsSectionSign(second));
    }
}
