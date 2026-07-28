package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatPlayerNameMatcherTest {
    @Test
    void matchesNamesCaseInsensitivelyAtWordBoundaries() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("Alice", "alice-id");
        assertEquals("alice-id", ChatPlayerNameMatcher.find("<aLiCe> hello", names));
        assertNull(ChatPlayerNameMatcher.find("malice said hello", names));
    }

    @Test
    void prefersTheEarliestThenLongestName() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("Tom", "short");
        names.put("Tom_2", "long");
        names.put("Later", "later");
        assertEquals("long", ChatPlayerNameMatcher.find("Tom_2 told Later", names));
    }
}
