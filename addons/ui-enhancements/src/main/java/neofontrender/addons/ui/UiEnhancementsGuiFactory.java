package neofontrender.addons.ui;

import net.minecraft.client.gui.GuiScreen;
import neofontrender.client.gui.NeofontrenderConfigScreen;
import neofontrender.client.gui.NfrConfigGuiFactory;

/** Opens the first UI Enhancements category from Forge's Mod List. */
public final class UiEnhancementsGuiFactory extends NfrConfigGuiFactory {
    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return NeofontrenderConfigScreen.createConfigGuiForNamespace(parentScreen, NfrUiEnhancements.MOD_ID);
    }
}
