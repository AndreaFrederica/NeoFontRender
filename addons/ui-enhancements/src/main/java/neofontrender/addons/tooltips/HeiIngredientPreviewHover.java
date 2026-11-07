package neofontrender.addons.tooltips;

import net.minecraft.client.renderer.GlStateManager;
import neofontrender.addons.hover.HoverAnimationState;
import neofontrender.addons.hover.HoverEffectsConfigAccess;
import neofontrender.addons.hover.HoverEffectsRenderer;
import neofontrender.addons.hover.IngredientGridHoverTarget;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/** Per-preview hover state; no hard dependency on HEI and no state shared between pinned panels. */
public final class HeiIngredientPreviewHover {
    private final IdentityHashMap<Rectangle, HoverAnimationState> animations = new IdentityHashMap<>();
    private Rectangle current;
    private Point origin;
    private long lastFrame;

    public void beginFrame() {
        long now = System.nanoTime();
        // A hidden/unpinned preview must not resurrect highlights when shown again.
        if (now - lastFrame > 250_000_000L) clear();
        lastFrame = now;
        current = null;
        origin = null;
    }

    public void setOrigin(Point origin) {
        this.origin = origin;
    }

    public void capture(IngredientGridHoverTarget renderer) {
        if (!HoverEffectsConfigAccess.jeiIngredientGridEnabled()) {
            renderer.nfrUi$drawOriginalHighlight();
            return;
        }
        current = renderer.nfrUi$hoverArea();
        if (current != null) animations.computeIfAbsent(current, ignored -> new HoverAnimationState());
    }

    public void clear() {
        animations.clear();
        current = null;
    }

    public void draw() {
        if (origin == null || !HoverEffectsConfigAccess.jeiIngredientGridEnabled()) {
            clear();
            return;
        }
        if (animations.isEmpty()) return;
        // This pass runs after HEI's panel context has ended. Restore the caller's state as well
        // as the grid's original screen translation and Z, including for fading previous cells.
        try (ModernTooltipRenderer.CallerGlState ignored = ModernTooltipRenderer.CallerGlState.capture()) {
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(origin.x, origin.y, 300.0F);
                Iterator<Map.Entry<Rectangle, HoverAnimationState>> iterator = animations.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Rectangle, HoverAnimationState> entry = iterator.next();
                    boolean active = entry.getKey() == current;
                    HoverAnimationState animation = entry.getValue();
                    animation.update(active, HoverEffectsConfigAccess.slotEnterMillis(),
                            HoverEffectsConfigAccess.slotExitMillis());
                    if (!active && !animation.isVisible()) {
                        iterator.remove();
                        continue;
                    }
                    HoverEffectsRenderer.drawIngredientGridHighlight(entry.getKey(), animation.easedProgress());
                }
            } finally {
                GlStateManager.popMatrix();
            }
        }
    }
}
