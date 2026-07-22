package neofontrender.addons.tooltips;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.config.GuiUtils;

import java.util.ArrayDeque;
import java.util.Deque;

/** Narrow bridge for HEI tooltips that mix text with independently rendered item grids. */
public final class HeiTooltipCompat {
    private static final ThreadLocal<Deque<PanelState>> PANELS =
            ThreadLocal.withInitial(ArrayDeque::new);

    private HeiTooltipCompat() {}

    public static void begin(ItemStack stack) {
        PANELS.get().push(new PanelState(stack == null ? ItemStack.EMPTY : stack));
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
            GuiUtils.drawGradientRect(zLevel, left, top, right, bottom, startColor, endColor);
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
            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
        }
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
