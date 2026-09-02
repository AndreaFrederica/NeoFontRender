package neofontrender.addons.tooltips;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Narrow bridge for HEI tooltips that mix text with independently rendered item grids. */
public final class HeiTooltipCompat {
    private static final int COLLAPSED_FOOTER_HEIGHT = 14;
    private static final ThreadLocal<Deque<PanelState>> PANELS =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static volatile CollapsedAccess collapsedAccess;
    private static volatile boolean collapsedAccessResolved;

    private HeiTooltipCompat() {}

    public static void begin(ItemStack stack) {
        PANELS.get().push(new PanelState(stack == null ? ItemStack.EMPTY : stack));
    }

    public static void beginIfAbsent(ItemStack stack) {
        if (PANELS.get().isEmpty()) begin(stack);
    }

    public static void beginCollapsed(Object renderer, Minecraft minecraft) {
        CollapsedInfo info = collapsedInfo(renderer);
        if (info == null || info.ingredientCount <= 1) return;
        FontRenderer font = minecraft == null ? null : minecraft.fontRenderer;
        PANELS.get().push(new PanelState(info.stack, info.modName, font));
    }

    public static void end() {
        Deque<PanelState> panels = PANELS.get();
        if (!panels.isEmpty()) panels.pop();
        if (panels.isEmpty()) PANELS.remove();
    }

    public static void finishCollapsed() {
        Deque<PanelState> panels = PANELS.get();
        if (panels.isEmpty()) return;
        PanelState state = panels.peek();
        if (state.collapsed && state.gradientCalls >= 2) {
            int contentX = state.left + 3;
            int contentWidth = Math.max(1, state.right - state.left - 6);
            ModernTooltipRenderer.drawCompatibleDivider(
                    contentX, state.top + 13, contentWidth, state.stack);
            if (state.font != null && !state.modName.isEmpty()) {
                String text = TextFormatting.BLUE.toString() + TextFormatting.ITALIC
                        + state.modName;
                state.font.drawStringWithShadow(text, contentX, state.originalBottom + 2,
                        TooltipConfig.textColor);
            }
        }
        end();
    }

    public static boolean isCustomTooltipActive() {
        return !PANELS.get().isEmpty();
    }

    public static int availableScreenWidth(int screenWidth) {
        PanelState state = activeOwnedPanel();
        return state == null ? screenWidth
                : Math.max(1, screenWidth - state.extents.right);
    }

    public static int availableScreenHeight(int screenHeight) {
        PanelState state = activeOwnedPanel();
        if (state == null) return screenHeight;
        int footer = state.collapsed && !state.modName.isEmpty()
                ? COLLAPSED_FOOTER_HEIGHT : 0;
        return Math.max(1, screenHeight - state.extents.bottom - footer);
    }

    private static PanelState activeOwnedPanel() {
        Deque<PanelState> panels = PANELS.get();
        if (panels.isEmpty() || !TooltipConfig.enabled || !TooltipConfig.heiCustomTooltips
                || !Arc3DRuntimeSupport.isAvailable()) {
            return null;
        }
        return panels.peek();
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
            if (state.collapsed && state.font != null && !state.modName.isEmpty()) {
                int footerRight = state.left + 6 + state.font.getStringWidth(
                        TextFormatting.ITALIC + state.modName);
                state.right = Math.max(state.right, footerRight);
            }
        } else if (call == 1) {
            state.originalBottom = bottom;
            int panelBottom = bottom + (state.collapsed && !state.modName.isEmpty()
                    ? COLLAPSED_FOOTER_HEIGHT : 0);
            ModernTooltipRenderer.drawCompatibleBackground(
                    state.left, state.top, state.right - state.left,
                    panelBottom - state.top, state.stack);

            // HEI deliberately keeps these disabled while it draws text and then enables depth
            // only for its item grid. The shared renderer restores normal GUI state, so reinstate
            // HEI's expected state before returning to its method.
            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
        }
    }

    private static CollapsedInfo collapsedInfo(Object renderer) {
        if (renderer == null) return null;
        CollapsedAccess access = collapsedAccess(renderer.getClass());
        if (access == null) return null;
        try {
            Object group = access.getCollapsedStack.invoke(renderer);
            if (group == null) return null;
            @SuppressWarnings("unchecked")
            List<Object> ingredients = (List<Object>) access.getDisplayIngredients.invoke(group);
            int count = ingredients == null ? 0 : ingredients.size();
            String modName = String.valueOf(access.getModNameForSorting.invoke(group));
            ItemStack stack = ItemStack.EMPTY;
            if (count > 0) {
                Object ingredient = access.getIngredient.invoke(ingredients.get(0));
                if (ingredient instanceof ItemStack) stack = (ItemStack) ingredient;
            }
            return new CollapsedInfo(count, stack,
                    "null".equals(modName) ? "" : modName.trim());
        } catch (ReflectiveOperationException | ClassCastException | LinkageError ignored) {
            return null;
        }
    }

    private static CollapsedAccess collapsedAccess(Class<?> rendererClass) {
        if (collapsedAccessResolved) return collapsedAccess;
        synchronized (HeiTooltipCompat.class) {
            if (collapsedAccessResolved) return collapsedAccess;
            try {
                Method getCollapsedStack = rendererClass.getMethod("getCollapsedStack");
                Class<?> groupClass = getCollapsedStack.getReturnType();
                Method getDisplayIngredients = groupClass.getMethod("getDisplayIngredients");
                Method getModNameForSorting = groupClass.getMethod("getModNameForSorting");
                Class<?> elementClass = Class.forName(
                        "mezz.jei.gui.ingredients.IIngredientListElement", false,
                        rendererClass.getClassLoader());
                collapsedAccess = new CollapsedAccess(getCollapsedStack, getDisplayIngredients,
                        getModNameForSorting, elementClass.getMethod("getIngredient"));
            } catch (ReflectiveOperationException | LinkageError ignored) {
                collapsedAccess = null;
            }
            collapsedAccessResolved = true;
            return collapsedAccess;
        }
    }

    private static final class PanelState {
        final ItemStack stack;
        final String modName;
        final FontRenderer font;
        final boolean collapsed;
        final TooltipVisualExtents extents;
        int gradientCalls;
        int left;
        int top;
        int right;
        int originalBottom;

        PanelState(ItemStack stack) {
            this(stack, "", null);
        }

        PanelState(ItemStack stack, String modName, FontRenderer font) {
            this.stack = stack;
            this.modName = modName == null ? "" : modName;
            this.font = font;
            this.collapsed = font != null;
            this.extents = TooltipVisualExtents.current();
        }
    }

    private static final class CollapsedInfo {
        final int ingredientCount;
        final ItemStack stack;
        final String modName;

        CollapsedInfo(int ingredientCount, ItemStack stack, String modName) {
            this.ingredientCount = ingredientCount;
            this.stack = stack;
            this.modName = modName;
        }
    }

    private static final class CollapsedAccess {
        final Method getCollapsedStack;
        final Method getDisplayIngredients;
        final Method getModNameForSorting;
        final Method getIngredient;

        CollapsedAccess(Method getCollapsedStack, Method getDisplayIngredients,
                        Method getModNameForSorting, Method getIngredient) {
            this.getCollapsedStack = getCollapsedStack;
            this.getDisplayIngredients = getDisplayIngredients;
            this.getModNameForSorting = getModNameForSorting;
            this.getIngredient = getIngredient;
        }
    }
}
