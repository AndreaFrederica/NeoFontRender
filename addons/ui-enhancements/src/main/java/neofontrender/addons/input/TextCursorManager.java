package neofontrender.addons.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.input.Mouse;

public final class TextCursorManager {
    private static long textCursor;

    private TextCursorManager() {}

    public static void beginFrame() {
        setCursor(0L);
    }

    public static void textFieldDrawn(int x, int y, int width, int height, boolean visible, boolean enabled) {
        if (!TextInputConfig.iBeamCursor || !visible || !enabled) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0) return;
        ScaledResolution resolution = new ScaledResolution(mc);
        int mouseX = Mouse.getX() * resolution.getScaledWidth() / mc.displayWidth;
        int mouseY = resolution.getScaledHeight() - Mouse.getY() * resolution.getScaledHeight() / mc.displayHeight - 1;
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            if (textCursor == 0L) textCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
            if (textCursor != 0L) setCursor(textCursor);
        }
    }

    public static void restoreDefault() {
        setCursor(0L);
    }

    public static void modularTextFieldDrawn(boolean hovering) {
        if (!TextInputConfig.iBeamCursor || !hovering) return;
        if (textCursor == 0L) textCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
        if (textCursor != 0L) setCursor(textCursor);
    }

    private static void setCursor(long cursor) {
        if (!TextInputConfig.iBeamCursor && cursor != 0L) return;
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0L) return;
        GLFW.glfwSetCursor(window, cursor);
    }
}
