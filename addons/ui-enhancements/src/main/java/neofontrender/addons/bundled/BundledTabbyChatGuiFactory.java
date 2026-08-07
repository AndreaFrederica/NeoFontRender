package neofontrender.addons.bundled;

import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.client.gui.NeofontrenderConfigScreen;
import neofontrender.client.gui.NfrConfigGuiFactory;

/** Routes the embedded TabbyChat Mod List entry to its NFR settings category. */
public final class BundledTabbyChatGuiFactory extends NfrConfigGuiFactory {
    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return NeofontrenderConfigScreen.createConfigGui(
                parentScreen, NfrUiEnhancements.MOD_ID + ":tabbed_chat");
    }
}
