package neofontrender.addons.bundled;

import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.client.gui.NeofontrenderConfigScreen;
import neofontrender.client.gui.NfrConfigGuiFactory;

/** Routes the embedded Salutation Mod List entry to the enhanced-chat category. */
public final class BundledSalutationGuiFactory extends NfrConfigGuiFactory {
    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return NeofontrenderConfigScreen.createConfigGui(
                parentScreen, NfrUiEnhancements.MOD_ID + ":enhanced_chat");
    }
}
