package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnhancedChatConfigAccessTest {
    @Test
    void restoredHistoryLoggingIsDisabledByDefault() {
        assertFalse(EnhancedChatConfigAccess.logRestoredHistory());
    }

    @Test
    void vanillaChatKeepsExtendedHistoryWhenTabbedChatIsDisabled() {
        boolean oldEnabled = EnhancedChatConfig.enabled;
        boolean oldTabbed = EnhancedChatConfig.tabbedChat;
        boolean oldExtended = EnhancedChatConfig.extendedHistory;
        boolean oldPersistence = EnhancedChatConfig.persistence;
        int oldLimit = EnhancedChatConfig.maxMessages;
        try {
            EnhancedChatConfig.enabled = true;
            EnhancedChatConfig.tabbedChat = false;
            EnhancedChatConfig.extendedHistory = true;
            EnhancedChatConfig.persistence = true;
            EnhancedChatConfig.maxMessages = 4096;

            assertFalse(EnhancedChatConfigAccess.tabbedChatEnabled(false));
            assertEquals(4096, EnhancedChatConfigAccess.messageLimit(false));
            assertTrue(EnhancedChatConfigAccess.persistenceEnabled(false));
        } finally {
            EnhancedChatConfig.enabled = oldEnabled;
            EnhancedChatConfig.tabbedChat = oldTabbed;
            EnhancedChatConfig.extendedHistory = oldExtended;
            EnhancedChatConfig.persistence = oldPersistence;
            EnhancedChatConfig.maxMessages = oldLimit;
        }
    }
}
