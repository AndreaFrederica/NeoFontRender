package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import neofontrender.api.client.tooltip.NfrTooltipApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Retained visual geometry; measurement, painting and diagnostics share one snapshot. */
final class TooltipVisualNodeLayout {
    private TooltipVisualNodeLayout() {}

    static Composition compose(NfrTooltipApi.VisualNode node, FontRenderer font) {
        return new Composition(adaptEntry(node, font));
    }

    static TooltipLayoutEngine.Node adapt(NfrTooltipApi.VisualNode node, FontRenderer font) {
        return adaptEntry(node, font);
    }

    static TooltipLayoutEngine.Measurement measure(List<? extends NfrTooltipApi.VisualNode> nodes,
                                                   FontRenderer font, int maxWidth, int maxHeight) {
        List<TooltipLayoutEngine.Node> children = new ArrayList<>();
        if (nodes != null) for (NfrTooltipApi.VisualNode node : nodes) {
            if (node != null) children.add(adaptEntry(node, font));
        }
        return new TooltipLayoutEngine.Flow(TooltipLayoutEngine.Direction.COLUMN,
                TooltipLayoutEngine.Alignment.START, 0, TooltipLayoutEngine.Insets.none(), children)
                .measure(new TooltipLayoutEngine.Constraints(maxWidth, maxHeight));
    }

    /** Preview dimensions are captured once and reused when a document is constrained. */
    static final class Composition {
        private final Entry root;

        private Composition(Entry root) { this.root = root; }

        Result layout(int maxWidth) {
            int limit = Math.max(1, maxWidth);
            TooltipLayoutEngine.Measurement size = root.measure(
                    new TooltipLayoutEngine.Constraints(limit, Integer.MAX_VALUE));
            root.place(new TooltipLayoutEngine.Rect(0, 0, size.width, size.height));
            float scale = size.width > limit ? limit / (float) size.width : 1.0F;
            List<Paint> paint = new ArrayList<>();
            List<TooltipVisualBlock.DebugBounds> debug = new ArrayList<>();
            root.snapshot(paint, debug, scale);
            return new Result(size.width, size.height, scale, paint, debug);
        }
    }

    static final class Result {
        final int width;
        final int height;
        final float scale;
        final List<TooltipVisualBlock.DebugBounds> debugBounds;
        final boolean requiresModelState;
        private final List<Paint> paint;

        private Result(int width, int height, float scale, List<Paint> paint,
                       List<TooltipVisualBlock.DebugBounds> debugBounds) {
            this.width = (int) Math.ceil(width * (double) scale);
            this.height = (int) Math.ceil(height * (double) scale);
            this.scale = scale;
            this.paint = Collections.unmodifiableList(new ArrayList<>(paint));
            this.debugBounds = Collections.unmodifiableList(new ArrayList<>(debugBounds));
            boolean model = false;
            for (Paint command : paint) model |= command.painter.model;
            requiresModelState = model;
        }

        void draw(int x, int y, FontRenderer font) {
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(x, y, 0.0F);
                GlStateManager.scale(scale, scale, 1.0F);
                for (Paint command : paint) command.painter.draw(command.bounds.x, command.bounds.y, font);
            } finally {
                GlStateManager.popMatrix();
            }
        }
    }

    private static final class Entry implements TooltipLayoutEngine.Node {
        private final String label;
        private final TooltipLayoutEngine.Node geometry;
        private final List<Entry> children;
        private final Painter painter;
        private TooltipLayoutEngine.Rect bounds = new TooltipLayoutEngine.Rect(0, 0, 0, 0);

        private Entry(String label, TooltipLayoutEngine.Node geometry, List<Entry> children, Painter painter) {
            this.label = label;
            this.geometry = geometry;
            this.children = children;
            this.painter = painter;
        }

        @Override public TooltipLayoutEngine.Measurement measure(TooltipLayoutEngine.Constraints constraints) {
            return geometry.measure(constraints);
        }

        @Override public void place(TooltipLayoutEngine.Rect bounds) {
            this.bounds = bounds;
            geometry.place(bounds);
        }

        private void snapshot(List<Paint> paint, List<TooltipVisualBlock.DebugBounds> debug, float scale) {
            debug.add(debugRect(label, bounds.x, bounds.y, bounds.width, bounds.height, scale));
            if (painter != null) {
                paint.add(new Paint(bounds, painter));
                if (painter.previewSize != null) {
                    debug.add(debugRect("preview:model", bounds.x + painter.insets.left(),
                            bounds.y + painter.insets.top(), painter.previewSize.width(),
                            painter.previewSize.height(), scale));
                }
            }
            for (Entry child : children) child.snapshot(paint, debug, scale);
        }
    }

    private static TooltipVisualBlock.DebugBounds debugRect(String label, int x, int y,
                                                             int width, int height, float scale) {
        int left = (int) Math.floor(x * (double) scale);
        int top = (int) Math.floor(y * (double) scale);
        int right = (int) Math.ceil((x + width) * (double) scale);
        int bottom = (int) Math.ceil((y + height) * (double) scale);
        return new TooltipVisualBlock.DebugBounds(label, left, top, right - left, bottom - top);
    }

    private static Entry adaptEntry(NfrTooltipApi.VisualNode node, FontRenderer font) {
        if (node == null) return leaf("empty", 0, 0, null);
        if (node instanceof NfrTooltipApi.GroupNode) {
            NfrTooltipApi.GroupNode group = (NfrTooltipApi.GroupNode) node;
            List<Entry> children = adaptChildren(group.children(), font);
            boolean row = group.direction() == NfrTooltipApi.LayoutDirection.HORIZONTAL;
            TooltipLayoutEngine.Alignment alignment = group.alignment() == NfrTooltipApi.LayoutAlignment.CENTER
                    ? TooltipLayoutEngine.Alignment.CENTER : group.alignment() == NfrTooltipApi.LayoutAlignment.END
                    ? TooltipLayoutEngine.Alignment.END : TooltipLayoutEngine.Alignment.START;
            return new Entry(row ? "group:row" : "group:column", new TooltipLayoutEngine.Flow(
                    row ? TooltipLayoutEngine.Direction.ROW : TooltipLayoutEngine.Direction.COLUMN,
                    alignment, group.gap(), TooltipLayoutEngine.Insets.none(), children), children, null);
        }
        if (node instanceof NfrTooltipApi.GridNode) {
            NfrTooltipApi.GridNode grid = (NfrTooltipApi.GridNode) node;
            List<Entry> children = adaptChildren(grid.children(), font);
            return new Entry("grid", new TooltipLayoutEngine.Grid(grid.columns(), grid.gap(), children),
                    children, null);
        }
        if (node instanceof NfrTooltipApi.ItemRowNode) {
            NfrTooltipApi.ItemRowNode row = (NfrTooltipApi.ItemRowNode) node;
            List<Entry> children = adaptChildren(row.items(), font);
            if (row.hasMore()) children.add(leaf("item-row:more",
                    font == null ? 10 : Math.max(1, font.getStringWidth("...") + 2), 17, Painter.more()));
            return new Entry("item-row:wrap", new TooltipLayoutEngine.Wrap(Integer.MAX_VALUE, 0, 0,
                    TooltipLayoutEngine.Insets.none(), children), children, null);
        }
        if (node instanceof NfrTooltipApi.PreviewNode) return preview((NfrTooltipApi.PreviewNode) node, font);
        String label = node instanceof NfrTooltipApi.ItemNode ? "item"
                : node instanceof NfrTooltipApi.TextNode ? "text" : "spacer";
        return leaf(label, node.width(font), node.height(font), new Painter(node));
    }

    private static List<Entry> adaptChildren(List<? extends NfrTooltipApi.VisualNode> nodes, FontRenderer font) {
        List<Entry> result = new ArrayList<>();
        for (NfrTooltipApi.VisualNode node : nodes) if (node != null) result.add(adaptEntry(node, font));
        return result;
    }

    private static Entry preview(NfrTooltipApi.PreviewNode preview, FontRenderer font) {
        int width = preview.width(font);
        int height = preview.height(font);
        String label = "preview:custom";
        try {
            NfrTooltipApi.PreviewRequest request = preview.request();
            if (request == null) return leaf(label, width, height, null);
            if (request.previewKind() == NfrTooltipApi.PreviewKind.ARMOR) label = "preview:armor";
            else if (request.previewKind() == NfrTooltipApi.PreviewKind.ITEM_STACK) label = "preview:item_stack";
            NfrTooltipApi.PreviewRenderer renderer = TooltipPreviewRenderers.find(request);
            if (renderer == null) return leaf(label, width, height, null);
            NfrTooltipApi.PreviewSize measured = renderer.measure(request, font);
            if (measured == null) return leaf(label, width, height, null);
            NfrTooltipApi.PreviewSize size = new NfrTooltipApi.PreviewSize(
                    width > 0 ? width : measured.width(), height > 0 ? height : measured.height());
            NfrTooltipApi.PreviewInsets insets = PreviewEffects.outsets(request, size);
            return leaf(label, size.width() + insets.left() + insets.right(),
                    size.height() + insets.top() + insets.bottom(), new Painter(preview, renderer, size, insets));
        } catch (RuntimeException | LinkageError ignored) {
            return leaf(label, width, height, null);
        }
    }

    private static Entry leaf(String label, int width, int height, Painter painter) {
        return new Entry(label, new TooltipLayoutEngine.Leaf(width, height), Collections.emptyList(), painter);
    }

    private static final class Paint {
        final TooltipLayoutEngine.Rect bounds;
        final Painter painter;
        Paint(TooltipLayoutEngine.Rect bounds, Painter painter) { this.bounds = bounds; this.painter = painter; }
    }

    /** Primitive drawing only; all positions and preview dimensions come from the retained snapshot. */
    private static final class Painter {
        final NfrTooltipApi.VisualNode node;
        final NfrTooltipApi.PreviewRenderer renderer;
        final NfrTooltipApi.PreviewSize previewSize;
        final NfrTooltipApi.PreviewInsets insets;
        final boolean model;
        final boolean more;

        Painter(NfrTooltipApi.VisualNode node) {
            this(node, null, null, NfrTooltipApi.PreviewInsets.NONE, false);
        }

        Painter(NfrTooltipApi.PreviewNode node, NfrTooltipApi.PreviewRenderer renderer,
                NfrTooltipApi.PreviewSize previewSize, NfrTooltipApi.PreviewInsets insets) {
            this(node, renderer, previewSize, insets, false);
        }

        private Painter(NfrTooltipApi.VisualNode node, NfrTooltipApi.PreviewRenderer renderer,
                        NfrTooltipApi.PreviewSize previewSize, NfrTooltipApi.PreviewInsets insets, boolean more) {
            this.node = node;
            this.renderer = renderer;
            this.previewSize = previewSize;
            this.insets = insets;
            this.more = more;
            this.model = node instanceof NfrTooltipApi.ItemNode || renderer != null;
        }

        static Painter more() { return new Painter(null, null, null, NfrTooltipApi.PreviewInsets.NONE, true); }

        void draw(int x, int y, FontRenderer font) {
            if (more) {
                if (font != null) font.drawStringWithShadow("...", x + 2, y + 2, 0xFFFFFF);
            } else if (node instanceof NfrTooltipApi.ItemNode) {
                NfrTooltipApi.ItemNode item = (NfrTooltipApi.ItemNode) node;
                RenderItem items = Minecraft.getMinecraft().getRenderItem();
                if (items == null) return;
                items.renderItemAndEffectIntoGUI(item.stack(), x, y);
                if (item.showAmount()) items.renderItemOverlayIntoGUI(font, item.stack(), x, y,
                        Long.toString(item.amount()));
            } else if (node instanceof NfrTooltipApi.TextNode && font != null) {
                NfrTooltipApi.TextNode text = (NfrTooltipApi.TextNode) node;
                if (text.shadow()) font.drawStringWithShadow(text.text(), x, y, text.color());
                else font.drawString(text.text(), x, y, text.color());
            } else if (renderer != null) {
                try {
                    renderer.render(((NfrTooltipApi.PreviewNode) node).request(),
                            x + insets.left(), y + insets.top(), previewSize, font);
                } catch (RuntimeException | LinkageError ignored) {
                    // Optional model renderers must not break the surrounding tooltip.
                }
            }
        }
    }
}
