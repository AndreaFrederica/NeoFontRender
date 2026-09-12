package neofontrender.addons.tooltips;

import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

/** Shares final geometry with Legendary's PostText decorations without replacing its effects. */
public final class LegendaryTooltipCompat {
    private static final ThreadLocal<Bounds> ACTIVE = new ThreadLocal<>();

    private LegendaryTooltipCompat() {}

    public static boolean prefersPanel() {
        boolean resourceFrame = LegendaryResourceCompat.INSTANCE.frames().stream().findFirst().isPresent();
        return TooltipConfig.enabled && TooltipPanelOwner.choose(Loader.isModLoaded("legendarytooltips"),
                resourceFrame, false, TooltipConfig.legendaryOwnership,
                TooltipConfig.yieldToLegendaryTooltips, false) == TooltipPanelOwner.LEGENDARY;
    }

    public static boolean prefersPanel(ItemStack stack) {
        boolean resourceFrame = LegendaryResourceCompat.INSTANCE.match(stack) != null;
        return TooltipConfig.enabled && TooltipPanelOwner.choose(Loader.isModLoaded("legendarytooltips"),
                resourceFrame, false, TooltipConfig.legendaryOwnership,
                TooltipConfig.yieldToLegendaryTooltips, false) == TooltipPanelOwner.LEGENDARY;
    }

    public static boolean hasLayout() { return ACTIVE.get() != null; }
    static Bounds bounds() { return ACTIVE.get(); }

    public static Scope begin(int left, int top, int right, int bottom) {
        Bounds previous = ACTIVE.get();
        ACTIVE.set(new Bounds(left + 3, top + 3,
                Math.max(1, right - left - 6), Math.max(1, bottom - top - 6)));
        return new Scope(previous);
    }

    public static RenderTooltipEvent.PostText align(RenderTooltipEvent.PostText event) {
        Bounds bounds = ACTIVE.get();
        if (bounds == null) return event;
        return new RenderTooltipEvent.PostText(event.getStack(), event.getLines(),
                bounds.x, bounds.y, event.getFontRenderer(), bounds.width, bounds.height);
    }

    /** Forge's rectangular panel, colored by Legendary's ordinary Color subscriber. */
    static void drawPanel(int left, int top, int right, int bottom, int fill, int start, int end) {
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();
        try {
            GuiUtils.drawGradientRect(300, left, top, right, bottom, fill, fill);
            GuiUtils.drawGradientRect(300, left, top, right, top + 1, start, start);
            GuiUtils.drawGradientRect(300, left, bottom - 1, right, bottom, end, end);
            GuiUtils.drawGradientRect(300, left, top + 1, left + 1, bottom - 1, start, end);
            GuiUtils.drawGradientRect(300, right - 1, top + 1, right, bottom - 1, start, end);
        } finally {
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            RenderHelper.enableGUIStandardItemLighting();
        }
    }

    static void drawResourceFrame(int left, int top, int right, int bottom, LegendaryResourceCompat.Frame frame) {
        if (frame == null || right <= left || bottom <= top) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(frame.image());
        int index = Math.max(0, frame.index());
        int u = (index % 8) * 64;
        int v = (index * 16) % 128;
        int size = Math.min(8, Math.min(right - left, bottom - top) / 2);
        if (size <= 0) return;
        Gui.drawModalRectWithCustomSizedTexture(left, top, u, v, size, size, 128, 128);
        Gui.drawModalRectWithCustomSizedTexture(right - size, top, u + 56, v, size, size, 128, 128);
        Gui.drawModalRectWithCustomSizedTexture(left, bottom - size, u, v + 8, size, size, 128, 128);
        Gui.drawModalRectWithCustomSizedTexture(right - size, bottom - size, u + 56, v + 8, size, size, 128, 128);
    }

    static final class Bounds {
        final int x, y, width, height;
        Bounds(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    public static final class Scope implements AutoCloseable {
        private final Bounds previous;
        private boolean closed;
        private Scope(Bounds previous) { this.previous = previous; }
        @Override public void close() {
            if (closed) return;
            closed = true;
            if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous);
        }
    }
}
