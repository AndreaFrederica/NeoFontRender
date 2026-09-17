package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;
import neofontrender.api.text.ModernText;

import java.util.Arrays;
import java.util.List;

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
    void commandColorsBuildOneCompleteModernLineWithoutAnOverlayCopy() {
        String line = "/tp -11340 88 -3728";
        List<CommandCompletionPresentation.ColoredRange> ranges = Arrays.asList(
                new CommandCompletionPresentation.ColoredRange(0, 3, 0xFF55FF55),
                new CommandCompletionPresentation.ColoredRange(4, 10, 0xFFFFAA00),
                new CommandCompletionPresentation.ColoredRange(11, 13, 0xFFFFFF55),
                new CommandCompletionPresentation.ColoredRange(14, 19, 0xFF55FFFF));

        CommandCompletionPresentation.StyledLine styled =
                CommandCompletionPresentation.styleLine(line, 0, ranges);
        StringBuilder rebuilt = new StringBuilder();
        for (ModernText.Run run : styled.modernText().runs()) rebuilt.append(run.text());

        assertEquals(line, rebuilt.toString());
        assertEquals(7, styled.modernText().runs().size());
        assertEquals(0x55FF55, styled.modernText().runs().get(0).rgb());
        assertFalse(styled.modernText().runs().get(1).hasColorOverride());
        assertEquals("\u00a7a/tp\u00a7r \u00a76-11340\u00a7r \u00a7e88\u00a7r \u00a7b-3728",
                styled.legacyText());
    }

    @Test
    void commandColorLayoutDoesNotRepeatTextWhenLaterRangesAreOnAnotherLine() {
        List<CommandCompletionPresentation.ColoredRange> ranges = Arrays.asList(
                new CommandCompletionPresentation.ColoredRange(0, 3, 0xFF55FF55),
                new CommandCompletionPresentation.ColoredRange(20, 26, 0xFFFFAA00));

        CommandCompletionPresentation.StyledLine styled =
                CommandCompletionPresentation.styleLine("/tp target", 0, ranges);
        StringBuilder rebuilt = new StringBuilder();
        for (ModernText.Run run : styled.modernText().runs()) rebuilt.append(run.text());

        assertEquals("/tp target", rebuilt.toString());
        assertEquals("\u00a7a/tp\u00a7r target", styled.legacyText());
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

    /**
     * Tab inserts a full option, so the next response for that same word is prefix-filtered down to
     * nothing. Keeping the list alive is what lets Tab keep walking the other options.
     */
    @Test
    void aCompletedOptionKeepsTheCandidateListForTheNextTab() {
        ChatCommandCompletionController.TokenRange committed =
                new ChatCommandCompletionController.TokenRange(10, 18);
        ChatCommandCompletionController.TokenRange current =
                new ChatCommandCompletionController.TokenRange(10, 18);

        // A prefix-filtered server list drops the finished word: cycle on the list Tab started from.
        assertTrue(ChatCommandCompletionController.keepsCommittedValue(
                committed, current, "creative", new String[0]));
        assertTrue(ChatCommandCompletionController.keepsCommittedValue(
                committed, current, "creative", new String[] {"survival"}));
    }

    @Test
    void aResponseThatStillOffersTheWordReplacesTheCycle() {
        ChatCommandCompletionController.TokenRange committed =
                new ChatCommandCompletionController.TokenRange(10, 18);
        ChatCommandCompletionController.TokenRange current =
                new ChatCommandCompletionController.TokenRange(10, 18);

        assertFalse(ChatCommandCompletionController.keepsCommittedValue(
                committed, current, "creative", new String[] {"creative", "comfort"}));
    }

    @Test
    void typingInsideTheCompletedWordEndsTheCycle() {
        ChatCommandCompletionController.TokenRange committed =
                new ChatCommandCompletionController.TokenRange(10, 18);
        ChatCommandCompletionController.TokenRange typed =
                new ChatCommandCompletionController.TokenRange(10, 14);

        assertFalse(ChatCommandCompletionController.keepsCommittedValue(
                committed, typed, "crea", new String[0]));
        assertFalse(ChatCommandCompletionController.keepsCommittedValue(
                committed, typed, "crea", new String[] {"creative"}));
    }

    @Test
    void anotherArgumentOrAnotherOptionIsNotPartOfTheCycle() {
        ChatCommandCompletionController.TokenRange committed =
                new ChatCommandCompletionController.TokenRange(10, 18);
        ChatCommandCompletionController.TokenRange nextWord =
                new ChatCommandCompletionController.TokenRange(19, 21);
        ChatCommandCompletionController.TokenRange sameWord =
                new ChatCommandCompletionController.TokenRange(10, 18);

        assertFalse(ChatCommandCompletionController.keepsCommittedValue(
                committed, nextWord, "tr", new String[0]));
        assertFalse(ChatCommandCompletionController.keepsCommittedValue(
                null, sameWord, "creative", new String[0]));
        assertFalse(ChatCommandCompletionController.keepsCommittedValue(
                committed, sameWord, "", new String[0]));
        assertTrue(ChatCommandCompletionController.keepsCommittedValue(
                committed, sameWord, "creative", new String[] {"survival"}));
    }

    @Test
    void committedRangeCoversTheInsertedValueOnly() {
        ChatCommandCompletionController.TokenRange typed =
                new ChatCommandCompletionController.TokenRange(7, 8);

        ChatCommandCompletionController.TokenRange committed = typed.tokens("creative");

        assertEquals(7, committed.start);
        assertEquals(15, committed.end);
        assertEquals(7, typed.tokens(null).end);
    }

    @Test
    void tokenRangeComparesByValueSoACycleCanBeMatchedToAResponse() {
        assertEquals(new ChatCommandCompletionController.TokenRange(7, 15),
                new ChatCommandCompletionController.TokenRange(7, 15));
        assertFalse(new ChatCommandCompletionController.TokenRange(7, 15)
                .equals(new ChatCommandCompletionController.TokenRange(7, 16)));
        assertEquals(new ChatCommandCompletionController.TokenRange(7, 15).hashCode(),
                new ChatCommandCompletionController.TokenRange(7, 15).hashCode());
    }

    /**
     * The regression that made every Tab after the first a no-op: the candidate list lived on the
     * popup state, so committing (which dismisses the popup) destroyed the list the next Tab needed.
     * A cycle outlives the popup, so Tab must walk the whole list and wrap around.
     */
    @Test
    void aCycleWalksEveryCandidateAndWrapsAround() {
        ChatCommandCompletionController.Cycle cycle = newCycle();

        // One Tab = commit the highlighted entry, then advance the highlight for the next Tab.
        // State.moveCycle() wraps this move with the viewport clamp; the walk itself lives here.
        String[] inserted = new String[5];
        for (int press = 0; press < inserted.length; press++) {
            inserted[press] = cycle.values().get(Math.max(0, cycle.selected()));
            cycle.onCommit();
            cycle.move(1);
        }

        assertEquals(Arrays.asList("creative", "survival", "adventure", "spectator", "creative"),
                Arrays.asList(inserted));
    }

    @Test
    void retreatFromTheFirstCandidateWrapsToTheLast() {
        ChatCommandCompletionController.Cycle cycle = newCycle();

        cycle.onCommit();
        cycle.move(-1);

        assertEquals(3, cycle.selected());
        assertEquals("spectator", cycle.values().get(cycle.selected()));
    }

    @Test
    void aCycleWithoutCandidatesNeverAdvances() {
        ChatCommandCompletionController.Cycle empty = new ChatCommandCompletionController.Cycle(
                new ChatCommandCompletionController.TokenRange(0, 1),
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList());

        empty.onCommit();
        empty.move(1);
        empty.move(-1);

        assertEquals(-1, empty.selected());
    }

    /**
     * The highlight used to leave the visible window because the viewport was only clamped while
     * building the cycle, never while moving it. Tab then inserted an off-screen candidate.
     */
    @Test
    void theViewportFollowsTheHighlightDownAndBackUp() {
        int size = 25;
        int visible = ChatSuggestionPopup.MAX_VISIBLE;

        // Walking down: the window only starts moving once the highlight passes the last visible row.
        assertEquals(0, ChatCommandCompletionController.viewportFirst(0, 0, size));
        assertEquals(0, ChatCommandCompletionController.viewportFirst(0, visible - 1, size));
        assertEquals(1, ChatCommandCompletionController.viewportFirst(0, visible, size));
        assertEquals(3, ChatCommandCompletionController.viewportFirst(0, visible + 2, size));

        // Moving back up past the top row pulls the window with it, pinning the highlight to the
        // first visible row. This is deliberately the same rule as the emoji popup's
        // keepSelectedVisible(), so both candidate lists scroll identically.
        assertEquals(2, ChatCommandCompletionController.viewportFirst(3, 2, size));
        // A short list caps how far the window may travel, so it stops before the last row.
        assertEquals(2, ChatCommandCompletionController.viewportFirst(3, 2, 12));
    }

    @Test
    void theViewportNeverScrollsWhenEverythingAlreadyFits() {
        // A list shorter than the window must never scroll, which is why the wheel felt dead on it.
        assertEquals(0, ChatCommandCompletionController.viewportFirst(0, 0, 8));
        assertEquals(0, ChatCommandCompletionController.viewportFirst(0, 7, 8));
        assertEquals(0, ChatCommandCompletionController.viewportFirst(0, 0, 0));
        assertEquals(0, ChatCommandCompletionController.viewportFirst(-5, 0, 8));
    }

    @Test
    void theViewportStaysInsideTheListAtTheBottom() {
        int size = 12;
        int visible = ChatSuggestionPopup.MAX_VISIBLE;

        int first = ChatCommandCompletionController.viewportFirst(0, size - 1, size);
        assertEquals(size - visible, first);
        assertTrue(first + visible <= size);
    }

    private static ChatCommandCompletionController.Cycle newCycle() {
        List<String> plain = Arrays.asList("creative", "survival", "adventure", "spectator");
        return new ChatCommandCompletionController.Cycle(
                new ChatCommandCompletionController.TokenRange(10, 10), plain, plain);
    }
}
