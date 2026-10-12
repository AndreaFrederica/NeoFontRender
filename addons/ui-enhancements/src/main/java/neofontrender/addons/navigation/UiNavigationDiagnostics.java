package neofontrender.addons.navigation;

import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.ui.NfrUiEnhancements;

final class UiNavigationDiagnostics {
    private UiNavigationDiagnostics() {}

    static void failure(String operation, GuiScreen screen, Throwable error) {
        String screenName = screen == null ? "<none>" : screen.getClass().getName();
        NfrUiEnhancements.LOGGER.error("UI navigation {} failed for {}", operation, screenName, error);
    }
}
