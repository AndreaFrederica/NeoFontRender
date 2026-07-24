package neofontrender.addons.tooltips;

import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayDeque;
import java.util.Deque;

/** Narrow bridge for HEI tooltips that mix text with independently rendered item grids. */
public final class HeiTooltipCompat {
    private static final ThreadLocal<Deque<PanelState>> PANELS =
            ThreadLocal.withInitial(ArrayDeque::new);

    private HeiTooltipCompat() {}

    public static void begin(ItemStack stack) {
        PANELS.get().push(new PanelState(stack));
    }

    public static void beginIfAbsent(ItemStack stack) {
        if (PANELS.get().isEmpty()) begin(stack);
    }

    public static void end() {
        Deque<PanelState> panels = PANELS.get();
        if (!panels.isEmpty()) panels.pop();
        if (panels.isEmpty()) PANELS.remove();
    }

    public static boolean isCustomTooltipActive() {
        return !PANELS.get().isEmpty();
    }

    /**
     * Replaces HEI's nine GuiUtils rectangles as one NFR panel. The first two calls are the
     * top and bottom strips, which together contain the complete bounds; the remaining calls
     * are suppressed. If compatibility is disabled, this is a transparent pass-through.
     */
    public static void drawGradientRect(int zLevel, int left, int top, int right, int bottom,
                                        int startColor, int endColor) {
        Deque<PanelState> panels = PANELS.get();
        if (panels.isEmpty() || !TooltipConfig.enabled || !TooltipConfig.heiCustomTooltips
                || !Arc3DRuntimeSupport.isAvailable()) {
            drawVanillaGradient(zLevel, left, top, right, bottom, startColor, endColor);
            return;
        }

        PanelState state = panels.peek();
        int call = state.gradientCalls++;
        if (call == 0) {
            state.left = left - 1;
            state.top = top;
            state.right = right + 1;
        } else if (call == 1) {
            ModernTooltipRenderer.drawCompatibleBackground(
                    state.left, state.top, state.right - state.left, bottom - state.top, state.stack);

            // HEI deliberately keeps these disabled while it draws text and then enables depth
            // only for its item grid. The shared renderer restores normal GUI state, so reinstate
            // HEI's expected state before returning to its method.
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
    }

    private static void drawVanillaGradient(
            int zLevel, int left, int top, int right, int bottom, int startColor, int endColor) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_QUADS);
        color(startColor);
        GL11.glVertex3f(right, top, zLevel);
        GL11.glVertex3f(left, top, zLevel);
        color(endColor);
        GL11.glVertex3f(left, bottom, zLevel);
        GL11.glVertex3f(right, bottom, zLevel);
        GL11.glEnd();
        GL11.glPopAttrib();
    }

    private static void color(int argb) {
        GL11.glColor4f((argb >> 16 & 255) / 255.0F, (argb >> 8 & 255) / 255.0F,
                (argb & 255) / 255.0F, (argb >>> 24) / 255.0F);
    }

    private static final class PanelState {
        final ItemStack stack;
        int gradientCalls;
        int left;
        int top;
        int right;

        PanelState(ItemStack stack) {
            this.stack = stack;
        }
    }
}
