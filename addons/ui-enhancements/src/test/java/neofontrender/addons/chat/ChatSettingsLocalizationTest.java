package neofontrender.addons.chat;

import neofontrender.addons.vendor.tabbychat.util.ChatVisibility;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatSettingsLocalizationTest {
    @Test
    void visibilityEnumsMapToLowercaseTranslationKeysWithoutChangingEnumNames() {
        for (ChatVisibility visibility : ChatVisibility.values()) {
            assertEquals("neofontrender_ui_enhancements.gui.tabby.visibility."
                            + visibility.name().toLowerCase(Locale.ROOT),
                    TabbedChatSettingsPage.visibilityKey(visibility.name()));
        }
    }
}
