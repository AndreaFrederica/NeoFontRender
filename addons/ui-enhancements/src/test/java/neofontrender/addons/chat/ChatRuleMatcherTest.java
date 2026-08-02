package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatRuleMatcherTest {
    @Test
    void matchesRegexAndIgnoresInvalidExpressions() {
        assertTrue(ChatRuleMatcher.matches("spam|advert", "Server advertisement"));
        assertFalse(ChatRuleMatcher.matches("[", "anything"));
    }

    @Test
    void playerListsAreCaseInsensitiveAndDeduplicated() {
        assertTrue(ChatRuleMatcher.containsName("Alice, Bob", "alice"));
        assertEquals("Alice, Bob", ChatRuleMatcher.addName("Alice, Bob", "ALICE"));
        assertEquals("Alice, Bob, Carol", ChatRuleMatcher.addName("Alice, Bob", "Carol"));
    }

    @Test
    void mentionRequiresAtSignAndNameBoundaries() {
        assertTrue(ChatRuleMatcher.mentioned("hello @Alice", "Alice"));
        assertFalse(ChatRuleMatcher.mentioned("hello Alice", "Alice"));
        assertFalse(ChatRuleMatcher.mentioned("hello @Alice2", "Alice"));
    }
}
