package neofontrender.addons.navigation.vanilla;

import com.cleanroommc.modularui.api.IMuiScreen;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.api.ui.navigation.UiTreeProvider;
import neofontrender.addons.api.ui.navigation.UiTreeSession;

public final class VanillaGuiTreeProvider implements UiTreeProvider {
    @Override public boolean supports(GuiScreen screen) { return !(screen instanceof IMuiScreen); }
    @Override public UiTreeSession open(GuiScreen screen) { return new VanillaGuiTreeSession(screen); }
}
