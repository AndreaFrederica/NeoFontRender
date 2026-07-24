package neofontrender.addons.tooltips;

import codechicken.lib.colour.LocalizedColours;
import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.guihook.GuiContainerManager;
import com.gtnewhorizon.gtnhlib.client.event.RenderTooltipEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** Integrates NFR tooltip rendering with GTNH NotEnoughItems' public tooltip event. */
public final class NeiTooltipCompat {
    public NeiTooltipCompat() {}

    @SubscribeEvent
    public void onRenderTooltip(RenderTooltipEvent event) {
        if (!shouldReplace(event)) return;

        // Custom-line rendering delegates the transparent panel back to NEI after NFR draws its panel.
        event.backgroundStart = 0;
        event.backgroundEnd = 0;
        event.borderStart = 0;
        event.borderEnd = 0;
        event.alternativeRenderer = lines -> render(event, lines);
    }

    private static boolean shouldReplace(RenderTooltipEvent event) {
        return TooltipConfig.enabled
                && TooltipConfig.neiCustomTooltips
                && Arc3DRuntimeSupport.isAvailable()
                && event.itemStack != null;
    }

    private static void render(RenderTooltipEvent event, List<String> lines) {
        if (hasCustomLines(lines)) {
            renderCustomLines(event, lines);
            return;
        }
        renderTextLines(event, lines);
    }

    private static void renderCustomLines(RenderTooltipEvent event, List<String> lines) {
        List<PageSelection> pages = paginate(lines);
        List<String> wrapped = new ArrayList<>(lines);
        for (PageSelection page : pages) {
            PageBounds bounds = PageBounds.measure(page.lines, event.font, page.index, pages.size());
            String first = page.lines.get(0);
            GuiDraw.ITooltipLineHandler original = GuiDraw.getTipLine(first);
            Dimension firstSize = lineSize(first, original, event.font, page.offset, lines.size());
            boolean onlyTextLine = page.lines.size() == 1 && original == null && pages.size() > 1
                    && !first.endsWith(GuiDraw.TOOLTIP_LINESPACE);
            if (onlyTextLine) firstSize.height += 2;
            PageStartLine wrapper = new PageStartLine(
                    event, first, original, firstSize, bounds);
            wrapped.set(page.offset, GuiDraw.TOOLTIP_HANDLER + GuiDraw.getTipLineId(wrapper));
        }
        GuiContainerManager.drawPagedTooltip(event.font, event.x + 12, event.y - 12, wrapped);
    }

    private static void renderTextLines(RenderTooltipEvent event, List<String> lines) {
        List<PageSelection> pages = paginate(lines);
        List<String> wrapped = new ArrayList<>(lines);
        for (PageSelection page : pages) {
            PageBounds bounds = PageBounds.measure(page.lines, event.font, page.index, pages.size());
            for (int index = 0; index < page.lines.size(); index++) {
                Dimension size = new Dimension(
                        event.font.getStringWidth(page.lines.get(index)),
                        paginationTextHeight(page.lines.get(index), page.offset + index, lines.size()));
                Dimension renderSize = new Dimension(size.width, bounds.renderLineHeights[index]);
                TextPageLine wrapper = new TextPageLine(
                        event, page.lines, new PhasedSize(size, renderSize), bounds,
                        index == 0, page.hasTitle(), pages.size() > 1);
                wrapped.set(page.offset + index,
                        GuiDraw.TOOLTIP_HANDLER + GuiDraw.getTipLineId(wrapper));
            }
        }
        GuiContainerManager.drawPagedTooltip(event.font, event.x + 12, event.y - 12, wrapped);
    }

    static List<PageSelection> paginate(List<String> lines) {
        return planPages(GuiContainerManager.splitTooltipByPage(lines));
    }

    static List<PageSelection> planPages(List<List<String>> split) {
        List<PageSelection> pages = new ArrayList<>(split.size());
        int offset = 0;
        for (int index = 0; index < split.size(); index++) {
            List<String> page = split.get(index);
            pages.add(new PageSelection(index, offset, page));
            offset += page.size();
        }
        return pages;
    }

    static boolean hasCustomLines(List<String> lines) {
        if (lines == null) return false;
        for (String line : lines) {
            if (line != null && line.startsWith(GuiDraw.TOOLTIP_HANDLER)) return true;
        }
        return false;
    }

    private static Dimension lineSize(
            String line, GuiDraw.ITooltipLineHandler handler, FontRenderer font, int index, int lineCount) {
        if (handler != null) return new Dimension(handler.getSize());
        return new Dimension(font.getStringWidth(line), paginationTextHeight(line, index, lineCount));
    }

    static int paginationTextHeight(String line, int index, int lineCount) {
        return line.endsWith(GuiDraw.TOOLTIP_LINESPACE) && index + 1 < lineCount ? 12 : 10;
    }

    static int renderedTextHeight(String line, int index, int pageSize, boolean paged) {
        if (index + 1 == pageSize && paged) return 12;
        return line.endsWith(GuiDraw.TOOLTIP_LINESPACE) && index + 1 < pageSize ? 12 : 10;
    }

    static final class PageSelection {
        final int index;
        final int offset;
        final List<String> lines;

        private PageSelection(int index, int offset, List<String> lines) {
            this.index = index;
            this.offset = offset;
            this.lines = lines;
        }

        boolean hasTitle() {
            return index == 0;
        }
    }

    static final class PhasedSize {
        private final Dimension pagination;
        private final Dimension render;
        private boolean paginationMeasured;

        PhasedSize(Dimension pagination, Dimension render) {
            this.pagination = pagination;
            this.render = render;
        }

        Dimension next() {
            // NEI measures every handler once while selecting a page, then remeasures the chosen page for drawing.
            if (!paginationMeasured) {
                paginationMeasured = true;
                return pagination;
            }
            return render;
        }
    }

    private static final class PageBounds {
        final int width;
        final int height;
        final int[] renderLineHeights;

        private PageBounds(int width, int height, int[] renderLineHeights) {
            this.width = width;
            this.height = height;
            this.renderLineHeights = renderLineHeights;
        }

        static PageBounds measure(List<String> page, FontRenderer font, int pageIndex, int pageCount) {
            int width = 0;
            int height = -2;
            int[] renderLineHeights = new int[page.size()];
            for (int index = 0; index < page.size(); index++) {
                String line = page.get(index);
                GuiDraw.ITooltipLineHandler handler = GuiDraw.getTipLine(line);
                Dimension size = handler == null
                        ? new Dimension(font.getStringWidth(line),
                                renderedTextHeight(line, index, page.size(), pageCount > 1))
                        : new Dimension(handler.getSize());
                width = Math.max(width, size.width);
                height += size.height;
                renderLineHeights[index] = size.height;
            }
            if (pageCount > 1) {
                String pageText = EnumChatFormatting.ITALIC + NEIClientUtils.translate(
                        "inventory.tooltip.page", pageIndex + 1, pageCount,
                        NEIClientConfig.getKeyName("gui.next_tooltip"));
                width = Math.max(width, font.getStringWidth(pageText));
                height += 10;
            }
            return new PageBounds(width, height, renderLineHeights);
        }
    }

    private static final class TextPageLine implements GuiDraw.ITooltipLineHandler {
        private final RenderTooltipEvent event;
        private final List<String> lines;
        private final PhasedSize size;
        private final PageBounds bounds;
        private final boolean pageStart;
        private final boolean hasTitle;
        private final boolean paged;

        private TextPageLine(RenderTooltipEvent event, List<String> lines, PhasedSize size,
                             PageBounds bounds, boolean pageStart, boolean hasTitle, boolean paged) {
            this.event = event;
            this.lines = lines;
            this.size = size;
            this.bounds = bounds;
            this.pageStart = pageStart;
            this.hasTitle = hasTitle;
            this.paged = paged;
        }

        @Override
        public Dimension getSize() {
            return size.next();
        }

        @Override
        public void draw(int x, int y) {
            if (pageStart) {
                ModernTooltipRenderer.drawCompatibleTextPage(
                        lines, x, y, bounds.width, bounds.height, bounds.renderLineHeights,
                        hasTitle, paged, event.itemStack, event.font);
            }
        }
    }

    private static final class PageStartLine implements GuiDraw.ITooltipLineHandler {
        private final RenderTooltipEvent event;
        private final String text;
        private final GuiDraw.ITooltipLineHandler delegate;
        private final Dimension size;
        private final PageBounds bounds;

        private PageStartLine(RenderTooltipEvent event, String text, GuiDraw.ITooltipLineHandler delegate,
                              Dimension size, PageBounds bounds) {
            this.event = event;
            this.text = text;
            this.delegate = delegate;
            this.size = size;
            this.bounds = bounds;
        }

        @Override
        public Dimension getSize() {
            return size;
        }

        @Override
        public void draw(int x, int y) {
            ModernTooltipRenderer.drawCompatibleBackground(
                    x - 4, y - 4, bounds.width + 7, bounds.height + 7, event.itemStack);
            if (delegate != null) delegate.draw(x, y);
            else event.font.drawStringWithShadow(text, x, y, LocalizedColours.TOOLTIP_TEXT);
        }
    }
}
