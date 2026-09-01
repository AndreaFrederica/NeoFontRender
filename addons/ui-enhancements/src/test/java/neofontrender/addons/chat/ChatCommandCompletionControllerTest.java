package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void findsTheWholeTokenAroundTheCursor() {
        ChatCommandCompletionController.TokenRange range =
                ChatCommandCompletionController.tokenRange("/gamemode creative target", 15);

        assertEquals(10, range.start);
        assertEquals(18, range.end);
        assertEquals(18, ChatCommandCompletionController.wordEnd(
                "/gamemode creative target", 13));
        assertEquals(12, ChatCommandCompletionController.wordEnd("/give\u3000player", 6));
    }

    @Test
    void insertionPreservesTheRootSlash() {
        ChatCommandCompletionController.TokenRange range =
                ChatCommandCompletionController.tokenRange("/gam", 4);

        assertEquals("/gamemode", ChatCommandCompletionController.insertionValue(
                "/gam", range, "gamemode"));
        assertEquals("/gamemode", ChatCommandCompletionController.insertionValue(
                "/gam", range, "/gamemode"));
    }

    @Test
    void replacementRangeConsumesTheRestOfTheCurrentToken() {
        String text = "/gamemode creative";
        ChatCommandCompletionController.TokenRange range =
                ChatCommandCompletionController.tokenRange(text, 15);
        String replacement = ChatCommandCompletionController.insertionValue(
                text, range, "survival");

        assertEquals("/gamemode survival",
                text.substring(0, range.start) + replacement + text.substring(range.end));
    }

    @Test
    void ghostTextUsesTheSelectedCandidateAndPreservesSlash() {
        assertEquals("emode", CommandCompletionPresentation.ghostSuffix(
                "/gam", 4, Arrays.asList("give", "gamemode"), 1));
        assertEquals("ive", CommandCompletionPresentation.ghostSuffix(
                "/gamemode creat", 16, Arrays.asList("creative"), -1));
        assertEquals("", CommandCompletionPresentation.ghostSuffix(
                "/gam", 2, Arrays.asList("gamemode"), 0));
    }

    @Test
    void requestTrackerDeduplicatesAndDropsStaleResponses() {
        ChatCommandCompletionController.RequestTracker tracker =
                new ChatCommandCompletionController.RequestTracker();
        assertTrue(tracker.shouldRequest("/g"));
        tracker.beginRequest("/g", new String[] { "ghost" });
        assertFalse(tracker.shouldRequest("/g"));
        tracker.beginRequest("/ga", new String[] { "gamemode" });

        assertNull(tracker.acceptResponse("/ga"));
        ChatCommandCompletionController.Request accepted = tracker.acceptResponse("/ga");
        assertEquals("/ga", accepted.prefix);
        assertEquals("gamemode", accepted.clientValues[0]);
        assertNull(tracker.acceptResponse("/ga"));
    }

    @Test
    void requestTrackerRejectsAResponseWhenTheInputChanged() {
        ChatCommandCompletionController.RequestTracker tracker =
                new ChatCommandCompletionController.RequestTracker();
        tracker.beginRequest("/give", new String[0]);

        assertNull(tracker.acceptResponse("/time"));
    }
}
