package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatCommandCompletionControllerTest {
    @Test
    void startsANewTokenAfterWhitespace() {
        assertEquals(0, ChatCommandCompletionController.wordStart("/give", 5));
        assertEquals(6, ChatCommandCompletionController.wordStart("/give ", 6));
        assertEquals(7, ChatCommandCompletionController.wordStart("/give  p", 8));
        assertEquals(6, ChatCommandCompletionController.wordStart("/give\u3000p", 7));
    }

    @Test
    void clampsCursorBeforeFindingToken() {
        assertEquals(6, ChatCommandCompletionController.wordStart("/give player", 99));
        assertEquals(0, ChatCommandCompletionController.wordStart("/give", -4));
        assertEquals(0, ChatCommandCompletionController.wordStart(null, 3));
    }
}
