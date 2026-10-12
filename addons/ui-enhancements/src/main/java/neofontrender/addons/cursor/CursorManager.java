package neofontrender.addons.cursor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import neofontrender.addons.ui.NfrUiEnhancements;
import me.eigenraven.lwjgl3ify.api.Lwjgl3Aware;
import org.lwjgl.input.Mouse;
import org.lwjgl.BufferUtils;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLPixels;
import org.lwjgl.sdl.SDLSurface;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Surface;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single GUI cursor owner. The world crosshair remains owned by CrosshairController.
 * All native handles are created, applied, and destroyed on the Minecraft client thread.
 *
 * <p>1.7.10 runs on lwjgl3ify, whose window stack is SDL (no GLFW at runtime), so native
 * cursor operations go through the LWJGL SDL binding. {@code @Lwjgl3Aware} keeps the
 * lwjgl3ify redirect transformer from rewriting these {@code org.lwjgl.sdl} references.
 */
@Lwjgl3Aware
public final class CursorManager {
    private static final Map<CursorType, Long> STANDARD_HANDLES = new EnumMap<>(CursorType.class);
    private static final Map<String, Long> CUSTOM_HANDLES = new HashMap<>();
    private static long appliedCursor = Long.MIN_VALUE;
    private static CursorRequest selected;
    private static CursorContext context;
    private static boolean frameActive;

    private CursorManager() {}

    public static void beginFrame(GuiScreen screen) {
        if (!CursorConfig.enabled || screen == null) {
            frameActive = false;
            selected = null;
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution =
                new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
        int mouseX = Mouse.getX() * resolution.getScaledWidth() / Math.max(1, minecraft.displayWidth);
        int mouseY = resolution.getScaledHeight()
                - Mouse.getY() * resolution.getScaledHeight() / Math.max(1, minecraft.displayHeight) - 1;
        context = new CursorContext(screen, mouseX, mouseY, Mouse.isButtonDown(0));
        selected = null;
        frameActive = true;
    }

    public static void endFrame() {
        if (!frameActive) {
            restoreDefault();
            return;
        }
        List<CursorRequest> rules = CursorRuleRegistry.resolve(context);
        for (CursorRequest request : rules) request(request);
        apply(selected == null ? CursorType.DEFAULT : selected.type());
        frameActive = false;
    }

    public static void request(CursorRequest request) {
        if (!frameActive || request == null || !CursorConfig.enabled) return;
        if (selected == null || request.priority() > selected.priority()
                || request.priority() == selected.priority()
                && request.source().compareTo(selected.source()) < 0) selected = request;
    }

    public static void request(CursorType type, CursorInteractionState state, int priority, String source) {
        request(CursorRequest.of(type, state, priority, source));
    }

    public static void textFieldDrawn(int x, int y, int width, int height,
                                      boolean visible, boolean enabled) {
        if (!CursorConfig.textFields || !visible || !enabled) return;
        if (contains(x, y, width, height)) {
            request(CursorType.TEXT, CursorInteractionState.HOVER, 500, "uie:vanilla_text_field");
        }
    }

    public static void modularTextFieldDrawn(boolean hovering) {
        if (CursorConfig.textFields && hovering)
            request(CursorType.TEXT, CursorInteractionState.HOVER, 500, "uie:modular_text_field");
    }

    public static void buttonDrawn(int x, int y, int width, int height,
                                   boolean visible, boolean enabled, boolean hovered) {
        if (!visible || !hovered) return;
        if (enabled && CursorConfig.buttons) {
            request(CursorType.BUTTON, CursorInteractionState.HOVER, 400, "uie:vanilla_button");
        } else if (!enabled && CursorConfig.disabledButtons) {
            request(CursorType.FORBIDDEN, CursorInteractionState.DISABLED, 450, "uie:disabled_button");
        }
    }

    public static void restoreDefault() {
        frameActive = false;
        selected = null;
        context = null;
        applyHandle(0L);
    }

    public static void dispose() {
        restoreDefault();
        if (SDLVideo.SDL_GL_GetCurrentContext() != 0L) {
            for (Long handle : STANDARD_HANDLES.values()) {
                if (handle != null && handle != 0L) SDLMouse.SDL_DestroyCursor(handle);
            }
        }
        STANDARD_HANDLES.clear();
        releaseCustomHandles();
    }

    private static boolean contains(int x, int y, int width, int height) {
        return context != null && context.mouseX() >= x && context.mouseX() < x + width
                && context.mouseY() >= y && context.mouseY() < y + height;
    }

    private static void apply(CursorType type) {
        String customId = CursorConfig.imageFor(type);
        long cursor = customId.isEmpty() ? standardHandle(type) : customHandle(customId);
        if (cursor == 0L && type != CursorType.DEFAULT) cursor = standardHandle(type);
        applyHandle(cursor);
    }

    private static void applyHandle(long cursor) {
        if (SDLVideo.SDL_GL_GetCurrentContext() == 0L) return;
        if (appliedCursor == cursor) return;
        long target = cursor == 0L ? SDLMouse.SDL_GetDefaultCursor() : cursor;
        if (target == 0L) return;
        SDLMouse.SDL_SetCursor(target);
        appliedCursor = cursor;
    }

    private static long standardHandle(CursorType type) {
        if (type == CursorType.DEFAULT) return 0L;
        if (type.sdlShape() < 0) return 0L;
        Long existing = STANDARD_HANDLES.get(type);
        if (existing != null) return existing;
        long created = SDLMouse.SDL_CreateSystemCursor(type.sdlShape());
        if (created != 0L) STANDARD_HANDLES.put(type, created);
        return created;
    }

    private static long customHandle(String id) {
        Long existing = CUSTOM_HANDLES.get(id);
        if (existing != null) return existing;
        CursorAsset asset = CursorAssetCatalog.INSTANCE.find(id);
        if (asset == null) return 0L;
        ByteBuffer pixels = BufferUtils.createByteBuffer(asset.width() * asset.height() * 4);
        for (int y = 0; y < asset.height(); y++) {
            for (int x = 0; x < asset.width(); x++) {
                int argb = asset.image().getRGB(x, y);
                pixels.put((byte) (argb >> 16));
                pixels.put((byte) (argb >> 8));
                pixels.put((byte) argb);
                pixels.put((byte) (argb >> 24));
            }
        }
        pixels.flip();
        SDL_Surface surface = SDLSurface.SDL_CreateSurfaceFrom(asset.width(), asset.height(),
                SDLPixels.SDL_PIXELFORMAT_RGBA8888, pixels, asset.width() * 4);
        if (surface == null) {
            NfrUiEnhancements.LOGGER.warn("Unable to create cursor surface for '{}'", id);
            return 0L;
        }
        try {
            long handle = SDLMouse.SDL_CreateColorCursor(surface, asset.hotspotX(), asset.hotspotY());
            if (handle != 0L) CUSTOM_HANDLES.put(id, handle);
            return handle;
        } catch (RuntimeException error) {
            NfrUiEnhancements.LOGGER.warn("Unable to create native cursor '{}'", id, error);
            return 0L;
        } finally {
            SDLSurface.SDL_DestroySurface(surface);
        }
    }

    static void releaseCustomHandles() {
        if (SDLVideo.SDL_GL_GetCurrentContext() != 0L) {
            if (CUSTOM_HANDLES.containsValue(appliedCursor)) {
                long defaultCursor = SDLMouse.SDL_GetDefaultCursor();
                if (defaultCursor != 0L) SDLMouse.SDL_SetCursor(defaultCursor);
            }
            for (Long handle : CUSTOM_HANDLES.values()) {
                if (handle != null && handle != 0L) SDLMouse.SDL_DestroyCursor(handle);
            }
        }
        CUSTOM_HANDLES.clear();
        appliedCursor = Long.MIN_VALUE;
    }

    static void logRuleFailure(String id, RuntimeException error) {
        NfrUiEnhancements.LOGGER.error("UIE cursor rule {} failed", id, error);
    }
}
