package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanroomCommandCompletionCompatTest {
    @Test
    void uieOwnsOnlyEnabledChatSuggestions() {
        assertTrue(CleanroomCommandCompletionCompat.shouldSuppress(false, true, true));
        assertFalse(CleanroomCommandCompletionCompat.shouldSuppress(true, true, true));
        assertFalse(CleanroomCommandCompletionCompat.shouldSuppress(false, false, true));
        assertFalse(CleanroomCommandCompletionCompat.shouldSuppress(false, true, false));
    }
}
