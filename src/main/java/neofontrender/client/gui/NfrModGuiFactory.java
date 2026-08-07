package neofontrender.client.gui;

import net.minecraft.client.gui.GuiScreen;
import neofontrender.client.gui.pages.NfrSettingsRoute;

/** Opens the main settings category from Forge's Mod List. */
public final class NfrModGuiFactory extends NfrConfigGuiFactory {
    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return NeofontrenderConfigScreen.createConfigGui(parentScreen, NfrSettingsRoute.FONT);
    }
}
