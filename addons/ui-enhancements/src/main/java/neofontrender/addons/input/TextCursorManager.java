package neofontrender.addons.input;

import neofontrender.addons.cursor.CursorManager;
import net.minecraft.client.gui.GuiScreen;

public final class TextCursorManager {
    private TextCursorManager() {}

    public static void beginFrame() { CursorManager.beginFrame(currentScreen()); }

    public static void endFrame() { CursorManager.endFrame(); }

    public static void textFieldDrawn(int x, int y, int width, int height, boolean visible, boolean enabled) {
        CursorManager.textFieldDrawn(x, y, width, height, visible, enabled);
    }

    public static void restoreDefault() {
        CursorManager.restoreDefault();
    }

    public static void modularTextFieldDrawn(boolean hovering) {
        CursorManager.modularTextFieldDrawn(hovering);
    }

    private static GuiScreen currentScreen() {
        return net.minecraft.client.Minecraft.getMinecraft().currentScreen;
    }
}
