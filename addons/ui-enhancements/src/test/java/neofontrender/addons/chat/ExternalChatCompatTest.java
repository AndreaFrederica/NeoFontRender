package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ExternalChatCompatTest {
    @Test
    public void ordinaryObjectsAreNotSalutationScreens() {
        assertFalse(ExternalChatCompat.isSalutationChatScreen(new Object()));
        assertFalse(ExternalChatCompat.isSalutationChatScreen(null));
    }
}
