package neofontrender.addons.cursor;

import me.eigenraven.lwjgl3ify.api.Lwjgl3Aware;
import org.lwjgl.sdl.SDLMouse;

/** Semantic cursor shapes. World reticles are deliberately outside this model. */
@Lwjgl3Aware
public enum CursorType {
    DEFAULT(SDLMouse.SDL_SYSTEM_CURSOR_DEFAULT),
    TEXT(SDLMouse.SDL_SYSTEM_CURSOR_TEXT),
    LINK(SDLMouse.SDL_SYSTEM_CURSOR_POINTER),
    BUTTON(SDLMouse.SDL_SYSTEM_CURSOR_POINTER),
    GRAB(SDLMouse.SDL_SYSTEM_CURSOR_POINTER),
    GRABBING(SDLMouse.SDL_SYSTEM_CURSOR_POINTER),
    CROSSHAIR(SDLMouse.SDL_SYSTEM_CURSOR_CROSSHAIR),
    RESIZE_HORIZONTAL(SDLMouse.SDL_SYSTEM_CURSOR_EW_RESIZE),
    RESIZE_VERTICAL(SDLMouse.SDL_SYSTEM_CURSOR_NS_RESIZE),
    RESIZE_DIAGONAL(SDLMouse.SDL_SYSTEM_CURSOR_DEFAULT),
    FORBIDDEN(SDLMouse.SDL_SYSTEM_CURSOR_NOT_ALLOWED),
    WAIT(SDLMouse.SDL_SYSTEM_CURSOR_WAIT),
    CUSTOM(-1);

    private final int sdlShape;

    CursorType(int sdlShape) { this.sdlShape = sdlShape; }
    int sdlShape() { return sdlShape; }
}
