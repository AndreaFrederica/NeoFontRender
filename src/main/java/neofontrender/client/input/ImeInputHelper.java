package neofontrender.client.input;

import neofontrender.core.config.NeofontrenderConfig;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCharCallbackI;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Stores the full Unicode codepoint from GLFW's character callback,
 * bypassing lwjglxx's i2c truncation that corrupts non-BMP characters.
 *
 * Uses a map keyed by the truncated char so timing between GLFW callbacks
 * and lwjglxx keyboard events doesn't matter.
 */
public final class ImeInputHelper {

    private static final Map<Character, Queue<Long>> pendingCodepoints = new ConcurrentHashMap<>();
    private static final int MAX_PENDING = 128;
    private static GLFWCharCallback previousCallback;
    private static boolean initialized;

    private ImeInputHelper() {
    }

    /**
     * Called from GLFW char callback. Stores the full codepoint keyed by
     * the truncated char that lwjglxx will put into the Keyboard queue.
     */
    private static void storeCodepoint(int codepoint) {
        // Prevent unbounded growth if handleKeyboardInput never consumes entries
        int total = 0;
        for (Queue<Long> q : pendingCodepoints.values()) {
            total += q.size();
        }
        if (total >= MAX_PENDING) {
            pendingCodepoints.clear();
            return;
        }
        char truncated = (char) codepoint;
        pendingCodepoints
                .computeIfAbsent(truncated, k -> new ConcurrentLinkedQueue<>())
                .offer((long) codepoint);
    }

    /**
     * Called from MixinGuiScreen when a keyboard character event arrives.
     * Resolves the truncated char to the full codepoint, or returns -1
     * if no mapping exists (normal BMP character).
     */
    public static long resolveCodepoint(char truncated) {
        Queue<Long> queue = pendingCodepoints.get(truncated);
        if (queue != null) {
            Long val = queue.poll();
            if (val != null) {
                return val;
            }
        }
        return truncated; // BMP character, no fix needed
    }

    public static synchronized void init(long windowHandle) {
        if (initialized) return;
        initialized = true;

        GLFWCharCallbackI previous = GLFW.glfwSetCharCallback(windowHandle, (window, codepoint) -> {
            long truncated = (long) (char) codepoint;
            if (codepoint > 0xFFFF) {
                storeCodepoint(codepoint);
            }
            if (NeofontrenderConfig.debugImeInput()) {
                System.out.println("[ImeInput] GLFW char: U+" + String.format("%04X", codepoint)
                        + " -> truncated U+" + String.format("%04X", truncated)
                        + (codepoint > 0xFFFF ? " [stored in map]" : "")
                        + " t=" + System.currentTimeMillis());
            }
            if (previousCallback != null) {
                previousCallback.invoke(window, codepoint);
            }
        });
        if (previous != null) {
            previousCallback = GLFWCharCallback.create(previous);
        }
        System.out.println("[ImeInput] GLFW char callback registered on window " + windowHandle);
    }
}
