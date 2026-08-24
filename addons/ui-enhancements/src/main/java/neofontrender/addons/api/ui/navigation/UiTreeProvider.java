package neofontrender.addons.api.ui.navigation;

import net.minecraft.client.gui.GuiScreen;

public interface UiTreeProvider {
    boolean supports(GuiScreen screen);
    UiTreeSession open(GuiScreen screen);
}
