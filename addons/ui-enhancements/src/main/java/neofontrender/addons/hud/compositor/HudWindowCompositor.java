package neofontrender.addons.hud.compositor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.IntBuffer;

/** Composes HUD surfaces with deterministic z-order, clipping, focus and pointer capture. */
public final class HudWindowCompositor {
    public static final HudWindowCompositor INSTANCE = new HudWindowCompositor();

    private final Map<String, HudSurface> surfaces = new LinkedHashMap<>();
    private final List<String> zOrder = new ArrayList<>();
    private String focused;
    private String pointerCapture;

    private HudWindowCompositor() {}

    public synchronized void register(HudSurface surface) {
        if (surface == null || surface.id() == null) return;
        surfaces.put(surface.id(), surface);
        if (!zOrder.contains(surface.id())) zOrder.add(surface.id());
    }

    public synchronized void unregister(String id) {
        HudSurface removed = surfaces.remove(id);
        zOrder.remove(id);
        if (id != null && id.equals(focused)) {
            if (removed != null) removed.focusChanged(false);
            focused = null;
        }
        if (id != null && id.equals(pointerCapture)) pointerCapture = null;
    }

    public synchronized void bringToFront(String id) {
        if (!surfaces.containsKey(id)) return;
        zOrder.remove(id);
        zOrder.add(id);
    }

    public void render(float partialTicks) {
        for (HudSurface surface : snapshot()) {
            if (!surface.visible()) continue;
            Rectangle clip = surface.bounds();
            if (clip == null || clip.width <= 0 || clip.height <= 0) continue;
            boolean scissorWasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            IntBuffer oldScissor = BufferUtils.createIntBuffer(4);
            if (scissorWasEnabled) GL11.glGetInteger(GL11.GL_SCISSOR_BOX, oldScissor);
            applyScissor(clip);
            try {
                surface.render(partialTicks);
            } finally {
                if (scissorWasEnabled) {
                    GL11.glScissor(oldScissor.get(0), oldScissor.get(1), oldScissor.get(2), oldScissor.get(3));
                } else {
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);
                }
            }
        }
    }

    public boolean mouseInput(int mouseX, int mouseY, int button, boolean pressed) {
        HudSurface target = surface(pointerCapture);
        if (target == null || !target.visible()) target = topAt(mouseX, mouseY);
        if (target == null || !target.acceptsPointer()) return false;
        if (pressed && button >= 0) {
            pointerCapture = target.id();
            focus(target.id());
            bringToFront(target.id());
        }
        target.mouseInput();
        if (!pressed && button >= 0) pointerCapture = null;
        return true;
    }

    private synchronized void focus(String id) {
        if (id.equals(focused)) return;
        HudSurface old = surfaces.get(focused);
        if (old != null) old.focusChanged(false);
        focused = id;
        HudSurface current = surfaces.get(focused);
        if (current != null) current.focusChanged(true);
    }

    private synchronized HudSurface topAt(int x, int y) {
        for (int i = zOrder.size() - 1; i >= 0; i--) {
            HudSurface surface = surfaces.get(zOrder.get(i));
            if (surface != null && surface.visible() && surface.bounds().contains(x, y)) return surface;
        }
        return null;
    }

    private synchronized HudSurface surface(String id) {
        return id == null ? null : surfaces.get(id);
    }

    private synchronized List<HudSurface> snapshot() {
        List<HudSurface> result = new ArrayList<>();
        for (String id : zOrder) {
            HudSurface surface = surfaces.get(id);
            if (surface != null) result.add(surface);
        }
        return result;
    }

    private static void applyScissor(Rectangle bounds) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc,
                mc.displayWidth, mc.displayHeight);
        int factor = resolution.getScaleFactor();
        int x = Math.max(0, bounds.x * factor);
        int y = Math.max(0, mc.displayHeight - (bounds.y + bounds.height) * factor);
        int width = Math.min(mc.displayWidth - x, Math.max(0, bounds.width * factor));
        int height = Math.min(mc.displayHeight - y, Math.max(0, bounds.height * factor));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, width, height);
    }
}
