package neofontrender.client;

import lumien.custommainmenu.gui.GuiCustom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomMainMenuIdentityTest {
    @Test void acceptsOnlyCmmTitleScreen() {
        assertTrue(NeofontrenderCustomMainMenu.isTitleScreen(new GuiCustom("mainmenu")));
        assertFalse(NeofontrenderCustomMainMenu.isTitleScreen(new GuiCustom("credits")));
        assertFalse(NeofontrenderCustomMainMenu.isTitleScreen(new GuiCustom("settings")));
        assertFalse(NeofontrenderCustomMainMenu.isTitleScreen(new Object()));
        assertFalse(NeofontrenderCustomMainMenu.isTitleScreen(null));
    }

    @Test void toleratesUninitializedConfig() {
        GuiCustom screen = new GuiCustom(null);
        assertFalse(NeofontrenderCustomMainMenu.isTitleScreen(screen));
        screen.guiConfig = null;
        assertFalse(NeofontrenderCustomMainMenu.isTitleScreen(screen));
    }
}
