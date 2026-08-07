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
    default void focusChanged(boolean focused) {}
}
