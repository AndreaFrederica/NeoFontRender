package neofontrender.addons.chat.network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatCharacterPolicyTest {
    @Test void sectionSignIsOnlyAllowedWithServerPermission() {
        assertFalse(ChatCharacterPolicy.isValid("hello\u00A7v", false));
        assertTrue(ChatCharacterPolicy.isValid("hello\u00A7v", true));
        assertFalse(ChatCharacterPolicy.isValid("hello\u0001", true));
    }

    @Test void filterPreservesOrdinaryAllowedCharacters() {
        assertEquals("hello\u00A7v", ChatCharacterPolicy.filter("hello\u00A7v", true));
        assertEquals("hellov", ChatCharacterPolicy.filter("hello\u00A7v", false));
    }
}
