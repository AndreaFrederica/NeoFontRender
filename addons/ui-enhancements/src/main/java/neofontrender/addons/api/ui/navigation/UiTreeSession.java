package neofontrender.addons.api.ui.navigation;

import net.minecraft.client.gui.GuiScreen;

public interface UiTreeSession extends AutoCloseable {
    GuiScreen screen();
    UiTreeSnapshot snapshot();
    UiActionResult perform(UiNodeId node, UiAction action);
    default UiActionResult pointerDown(int x, int y, int button) { return UiActionResult.IGNORED; }
    default UiActionResult pointerMove(int x, int y, int button, long timeSincePress) {
        return UiActionResult.IGNORED;
    }
    default UiActionResult pointerUp(int x, int y, int button) { return UiActionResult.IGNORED; }
    default UiActionResult pointerScroll(int x, int y, int wheel) { return UiActionResult.IGNORED; }
    UiActionResult reveal(UiNodeId node);
    UiActionResult back();
    void refresh();
    @Override void close();
}
