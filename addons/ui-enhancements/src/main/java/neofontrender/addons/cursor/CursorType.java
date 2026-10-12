package neofontrender.addons.cursor;

import org.lwjgl.glfw.GLFW;

/** Semantic cursor shapes. World reticles are deliberately outside this model. */
public enum CursorType {
    DEFAULT(GLFW.GLFW_ARROW_CURSOR),
    TEXT(GLFW.GLFW_IBEAM_CURSOR),
    LINK(GLFW.GLFW_HAND_CURSOR),
    BUTTON(GLFW.GLFW_HAND_CURSOR),
    GRAB(GLFW.GLFW_HAND_CURSOR),
    GRABBING(GLFW.GLFW_HAND_CURSOR),
    CROSSHAIR(GLFW.GLFW_CROSSHAIR_CURSOR),
    RESIZE_HORIZONTAL(GLFW.GLFW_HRESIZE_CURSOR),
    RESIZE_VERTICAL(GLFW.GLFW_VRESIZE_CURSOR),
    RESIZE_DIAGONAL(GLFW.GLFW_ARROW_CURSOR),
    FORBIDDEN(GLFW.GLFW_NOT_ALLOWED_CURSOR),
    WAIT(GLFW.GLFW_ARROW_CURSOR),
    CUSTOM(-1);

    private final int glfwShape;

    CursorType(int glfwShape) { this.glfwShape = glfwShape; }
    int glfwShape() { return glfwShape; }
}
