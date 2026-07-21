package neofontrender.client.gui;

import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import neofontrender.client.gui.pages.NfrEmojiTestPage;

/** Entry point for the emoji diagnostics page. */
@SideOnly(Side.CLIENT)
public final class NeofontrenderEmojiTestScreen {
    private NeofontrenderEmojiTestScreen() {}

    public static void open() {
        ModularPanel panel = new ModularPanel("emoji_test_panel").relativeToScreen().full();
        panel.child(new NfrEmojiTestPage().relativeToParent().full());
        ClientGUI.open(new ModularScreen("neofontrender_emoji_test", panel).pausesGame(false));
    }
}
