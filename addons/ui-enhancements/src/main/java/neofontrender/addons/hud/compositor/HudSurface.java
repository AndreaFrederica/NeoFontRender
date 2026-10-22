package neofontrender.addons.hud.compositor;

import java.awt.Rectangle;

/** A renderable, focusable surface hosted by the in-game HUD compositor. */
public interface HudSurface {
    String id();
    Rectangle bounds();
    boolean visible();
    void render(float partialTicks);

    default boolean acceptsPointer() { return true; }
    default void mouseInput() {}
    /** Pointer event in scaled-screen coordinates; the no-argument hook remains source-compatible. */
    default void mouseInput(int mouseX, int mouseY, int button, boolean pressed) { mouseInput(); }
    default void focusChanged(boolean focused) {}
}
