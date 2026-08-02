package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatSourceClassifierTest {
    @Test
    void explicitPatternsOverridePacketHints() {
        assertEquals(ChatSource.SERVER, ChatSourceClassifier.classify(true,
                "[公告] Alice joined", "Alice", "", "^\\[公告]", ""));
        assertEquals(ChatSource.PLAYER, ChatSourceClassifier.classify(false,
                "<Alice> hello", "Alice", "", "", "^<[^>]+>"));
    }

    @Test
    void systemMessagesDoNotBecomePlayerMessagesOnlyBecauseTheyMentionAPlayer() {
        assertEquals(ChatSource.SERVER, ChatSourceClassifier.classify(false,
                "Alice completed an advancement", "Alice", "", "", ""));
    }

    @Test
    void chatPacketsWithoutARecognizedSenderRemainServerMessages() {
        assertEquals(ChatSource.SERVER, ChatSourceClassifier.classify(true,
                "[Shop] Sale started", "", "", "", ""));
    }

    @Test
    void recognizesCommonPrivateMessages() {
        assertEquals(ChatSource.PRIVATE, ChatSourceClassifier.classify(false,
                "From Alice: hello", "Alice", "", "", ""));
        assertEquals(ChatSource.PRIVATE, ChatSourceClassifier.classify(false,
                "Alice whispers to you: hello", "Alice", "", "", ""));
    }
}
