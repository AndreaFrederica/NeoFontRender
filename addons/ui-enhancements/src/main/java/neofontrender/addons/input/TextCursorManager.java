package neofontrender.addons.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import neofontrender.addons.ui.NfrUiEnhancements;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;

import java.nio.IntBuffer;

/** Owns the LWJGL2 native I-beam cursor and frame-level hover arbitration. */
public final class TextCursorManager {
    private static Cursor textCursor;
    private static Cursor defaultCursor;
    private static boolean defaultCursorCaptured;
    private static boolean textCursorRequested;
    private static boolean unavailable;

    private TextCursorManager() {}

    public static void beginFrame() {
        textCursorRequested = false;
    }

    public static void endFrame() {
        setTextCursor(TextInputConfig.iBeamCursor && textCursorRequested);
    }

    public static void textFieldDrawn(int x, int y, int width, int height, boolean visible, boolean enabled) {
        if (!TextInputConfig.iBeamCursor || !visible || !enabled) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.displayWidth <= 0 || minecraft.displayHeight <= 0) return;
        ScaledResolution resolution = new ScaledResolution(
                minecraft, minecraft.displayWidth, minecraft.displayHeight);
        int mouseX = Mouse.getX() * resolution.getScaledWidth() / minecraft.displayWidth;
        int mouseY = resolution.getScaledHeight()
                - Mouse.getY() * resolution.getScaledHeight() / minecraft.displayHeight - 1;
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            textCursorRequested = true;
        }
    }

    public static void modularTextFieldDrawn(boolean hovered) {
        if (TextInputConfig.iBeamCursor && hovered) textCursorRequested = true;
    }

    public static void restoreDefault() {
        setTextCursor(false);
    }

    private static void setTextCursor(boolean active) {
        if (unavailable || !Mouse.isCreated()) return;
        try {
            if (active) {
                ensureCursor();
                if (textCursor != null && Mouse.getNativeCursor() != textCursor) Mouse.setNativeCursor(textCursor);
            } else if (defaultCursorCaptured && Mouse.getNativeCursor() == textCursor) {
                Mouse.setNativeCursor(defaultCursor);
            }
        } catch (LWJGLException exception) {
            unavailable = true;
            NfrUiEnhancements.LOGGER.error("Native I-beam cursor failed; disabling the input cursor module", exception);
        }
    }

    private static void ensureCursor() throws LWJGLException {
        if (textCursor != null) return;
        if ((Cursor.getCapabilities() & Cursor.CURSOR_8_BIT_ALPHA) == 0) {
            unavailable = true;
            NfrUiEnhancements.LOGGER.error("Native I-beam cursor requires CURSOR_8_BIT_ALPHA support");
            return;
        }
        defaultCursor = Mouse.getNativeCursor();
        defaultCursorCaptured = true;
        IntBuffer pixels = BufferUtils.createIntBuffer(16 * 16);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean ink = (y == 1 || y == 14) && x >= 5 && x <= 10
                        || x == 7 && y >= 2 && y <= 13
                        || x == 8 && y >= 2 && y <= 13;
                pixels.put(ink ? 0xFF000000 : 0x00000000);
            }
        }
        pixels.flip();
        textCursor = new Cursor(16, 16, 7, 8, 1, pixels, null);
    }
}
