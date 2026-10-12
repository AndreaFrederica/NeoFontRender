package neofontrender.addons.cursor;

import net.minecraft.client.gui.GuiScreen;

/** Immutable snapshot supplied to extension rules for one GUI frame. */
public final class CursorContext {
    private final GuiScreen screen;
    private final int mouseX;
    private final int mouseY;
    private final boolean leftButtonDown;

    public CursorContext(GuiScreen screen, int mouseX, int mouseY, boolean leftButtonDown) {
        this.screen = screen;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.leftButtonDown = leftButtonDown;
    }

    public GuiScreen screen() { return screen; }
    public int mouseX() { return mouseX; }
    public int mouseY() { return mouseY; }
    public boolean leftButtonDown() { return leftButtonDown; }
}
