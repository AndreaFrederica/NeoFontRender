package neofontrender.addons.mainmenu;

import net.minecraft.client.gui.GuiButton;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MainMenuContinueButtonTest {
    @Test void followsTheRealListAcrossRepeatedFramesAndReinitialization() {
        List<GuiButton> buttons = new ArrayList<>();
        MainMenuContinueButton first = button("first");
        CustomMainMenuButtonInjector.updateButton(buttons, true, () -> first);
        CustomMainMenuButtonInjector.updateButton(buttons, true, () -> { fail("duplicate creation"); return null; });
        assertEquals(1, buttons.size());
        assertSame(first, buttons.get(0));
        buttons.clear(); // CMM resize/reopen rebuilds the same list.
        MainMenuContinueButton second = button("second");
        CustomMainMenuButtonInjector.updateButton(buttons, true, () -> second);
        assertSame(second, buttons.get(0));
    }

    @Test void disablingRemovesOnlyOurButtonAndMissingTargetsDoNotAddOne() {
        List<GuiButton> buttons = new ArrayList<>();
        GuiButton foreign = new GuiButton(MainMenuContinueButton.BUTTON_ID, 0, 0, "Other mod");
        buttons.add(foreign);
        CustomMainMenuButtonInjector.updateButton(buttons, true, () -> null);
        assertEquals(1, buttons.size());
        CustomMainMenuButtonInjector.updateButton(buttons, true, () -> button("world"));
        assertEquals(2, buttons.size());
        CustomMainMenuButtonInjector.updateButton(buttons, false, () -> { fail("disabled creation"); return null; });
        assertEquals(1, buttons.size());
        assertSame(foreign, buttons.get(0));
    }

    private static MainMenuContinueButton button(String name) {
        return new MainMenuContinueButton(LastPlayedTarget.singleplayer(name, name), 0, 0, 200, name);
    }

    @Test void doesNotHijackAnotherModsButtonWithTheSameId() {
        assertFalse(MainMenuContinueButton.isContinueButton(
                new GuiButton(MainMenuContinueButton.BUTTON_ID, 0, 0, "Other mod")));
        assertFalse(MainMenuContinueButton.isContinueButton(null));
    }
}
