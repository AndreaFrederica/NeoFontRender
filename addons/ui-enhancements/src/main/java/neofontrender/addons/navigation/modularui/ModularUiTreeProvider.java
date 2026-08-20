package neofontrender.addons.navigation.modularui;

import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.overlay.OverlayStack;
import com.cleanroommc.modularui.screen.ModularScreen;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.api.ui.navigation.UiTreeProvider;
import neofontrender.addons.api.ui.navigation.UiTreeSession;

public final class ModularUiTreeProvider implements UiTreeProvider {
    @Override public boolean supports(GuiScreen screen) { return findScreen(screen) != null; }

    @Override public UiTreeSession open(GuiScreen screen) {
        ModularScreen modularScreen = findScreen(screen);
        if (modularScreen == null) throw new IllegalArgumentException("not a ModularUI screen");
        return new ModularUiTreeSession(screen, modularScreen);
    }

    private static ModularScreen findScreen(GuiScreen screen) {
        ModularScreen[] overlay = new ModularScreen[1];
        OverlayStack.foreach(candidate -> {
            if (overlay[0] == null && candidate.getScreenWrapper().getGuiScreen() == screen) {
                overlay[0] = candidate;
            }
        }, true);
        if (overlay[0] != null) return overlay[0];
        return screen instanceof IMuiScreen ? ((IMuiScreen) screen).getScreen() : null;
    }
}
