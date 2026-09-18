package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.Loader;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import neofontrender.addons.ui.NfrUiEnhancements;
import org.lwjgl.opengl.GL11;
import java.util.concurrent.atomic.AtomicLong;

/** Visual blocks anchored after real tooltip lines, independent of whitespace placeholders. */
final class TooltipVisualPlan {
    static final ThreadLocal<List<NfrTooltipApi.VisualNode>> EXTERNAL = new ThreadLocal<>();
    private static final long DIAGNOSTIC_INTERVAL_NANOS = 2_000_000_000L;
    private static final AtomicLong LAST_COLLECT_DIAGNOSTIC = new AtomicLong();
    private static final AtomicLong LAST_DRAW_DIAGNOSTIC = new AtomicLong();
    private static final AtomicLong LAST_ANCHOR_DIAGNOSTIC = new AtomicLong();
    private final List<List<TooltipVisualBlock>> afterLines;
    private final List<TooltipVisualBlock> sideBlocks = new ArrayList<>();
    private int sideWidthLimit = Integer.MAX_VALUE;

    TooltipVisualPlan(int lineCount) {
        afterLines = new ArrayList<>(Math.max(0, lineCount));
        for (int i = 0; i < lineCount; i++) afterLines.add(new ArrayList<>());
    }

    static TooltipVisualPlan collect(ItemStack stack, List<String> lines, FontRenderer font,
                                     int maxWidth) {
        TooltipVisualPlan plan = new TooltipVisualPlan(lines == null ? 0 : lines.size());
        List<NfrTooltipApi.VisualNode> external = EXTERNAL.get();
        if (lines != null && !lines.isEmpty() && external != null) {
            for (NfrTooltipApi.VisualNode node : external) {
                if (node == null) continue;
                ExternalBlock block = new ExternalBlock(node, font);
                if (node.kind() == NfrTooltipApi.Kind.PREVIEW) plan.addSide(block);
                else plan.addAfter(anchorFor(node, lines, stack), block);
            }
        }

        if (lines != null && !lines.isEmpty()) {
            ThaumcraftAspectTooltipCompat.Chart chart =
                    ThaumcraftAspectTooltipCompat.chart(stack, font, maxWidth);
            if (chart != null) plan.addAfter(0, chart);
            QuarkTooltipVisuals.populate(plan, stack, lines, font, maxWidth);
        }
        if (shouldDiagnostic(LAST_COLLECT_DIAGNOSTIC)) {
            NfrUiEnhancements.LOGGER.debug(
                    "Tooltip visual plan: lines={}, externalNodes={}, blocks={}, maxWidth={}, totalHeight={}, hasAny={}, maxConstraint={}",
                    lines == null ? 0 : lines.size(), external == null ? 0 : external.size(),
                    plan.blockCount(), plan.maxWidth(), plan.totalHeight(), plan.hasAny(), maxWidth);
        }
        return plan;
    }

    /**
     * Documents declare where their visual content belongs with an
     * {@code <nfr:anchor family="..."/>} marker line; content without a marker for its
     * family falls back to the ownership-line placement.
     */
    private static int anchorFor(NfrTooltipApi.VisualNode node, List<String> lines, ItemStack stack) {
        String family = familyOf(node.kind());
        if (family != null) {
            NfrTooltipAnchor.FamilyScan scan = NfrTooltipAnchor.scanFamily(lines, family);
            if (scan.found()) {
                if (shouldDiagnostic(LAST_ANCHOR_DIAGNOSTIC)) {
                    NfrUiEnhancements.LOGGER.debug(
                            "Tooltip anchor: family={}, line={}, producer={}, custom={}, duplicates={}, producers={}, unknown={}",
                            family, scan.firstIndex, scan.anchor.producer,
                            scan.anchor.custom == null ? "-" : scan.anchor.custom,
                            scan.count, scan.producers, scan.anchor.unknownAttributes);
                }
                return Math.max(0, Math.min(scan.firstIndex, lines.size() - 1));
            }
        }
        if (node.kind() == NfrTooltipApi.Kind.PREVIEW) return 0;
        return Math.max(0, Math.min(defaultAnchor(lines, stack), lines.size() - 1));
    }

    private static String familyOf(NfrTooltipApi.Kind kind) {
        if (kind == NfrTooltipApi.Kind.ITEM || kind == NfrTooltipApi.Kind.ITEM_ROW) {
            return NfrTooltipAnchor.FAMILY_ITEMS;
        }
        if (kind == NfrTooltipApi.Kind.TEXT) return NfrTooltipAnchor.FAMILY_TEXT;
        return null;
    }

    /**
     * External visuals without a family marker belong immediately before the final
     * ownership line, so ownership remains last.
     */
    private static int defaultAnchor(List<String> lines, ItemStack stack) {
        int anchor = lines.size() - 1;
        if (TooltipConfig.modNameEnabled && TooltipConfig.modNameMoveToEnd) {
            String owner = ModNameTooltipHandler.getModName(stack);
            if (owner != null) {
                String marker = ModNameTooltipSupport.format(TooltipConfig.modNameFormat) + owner;
                for (int i = lines.size() - 1; i >= 0; i--) {
                    if (lines.get(i) != null && lines.get(i).contains(marker)) {
                        anchor = Math.max(0, i - 1);
                        break;
                    }
                }
            }
        }
        return anchor;
    }

    private static final class ExternalBlock implements TooltipVisualBlock {
        private final NfrTooltipApi.VisualNode node;
        private final FontRenderer font;
        private final int maxWidth;

        ExternalBlock(NfrTooltipApi.VisualNode node, FontRenderer font) {
            this(node, font, Integer.MAX_VALUE);
        }

        private ExternalBlock(NfrTooltipApi.VisualNode node, FontRenderer font, int maxWidth) {
            this.node = node;
            this.font = font;
            this.maxWidth = maxWidth;
        }
        public int width() {
            return nodeWidth(node, font, maxWidth);
        }
        public int height() {
            return nodeHeight(node, font, maxWidth);
        }
        @Override public TooltipVisualBlock constrain(int width) {
            return new ExternalBlock(node, font, Math.max(1, width));
        }
        @Override public String debugLabel() {
            switch (node.kind()) {
                case ITEM: return "item";
                case ITEM_ROW: return "item-row";
                case PREVIEW:
                    if (node instanceof NfrTooltipApi.PreviewNode) {
                        NfrTooltipApi.PreviewKind kind = ((NfrTooltipApi.PreviewNode) node)
                                .request().previewKind();
                        if (kind == NfrTooltipApi.PreviewKind.ARMOR) return "preview:armor";
                        if (kind == NfrTooltipApi.PreviewKind.ITEM_STACK) return "preview:item_stack";
                    }
                    return "preview:custom";
                case GROUP: return "group";
                case GRID: return "grid";
                case TEXT: return "text";
                case SPACER: return "spacer";
                default: return "visual";
            }
        }
        public void draw(int x, int y, FontRenderer font) {
            boolean item = requiresModelState(node);
            if (shouldDiagnostic(LAST_DRAW_DIAGNOSTIC)) {
                RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
                boolean depth = false;
                boolean lighting = false;
                try {
                    depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
                    lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
                } catch (RuntimeException ignored) {
                    // Diagnostics must never interfere with rendering.
                }
                String stackInfo = "n/a";
                if (node instanceof NfrTooltipApi.ItemNode) {
                    ItemStack stack = ((NfrTooltipApi.ItemNode) node).stack();
                    if (stack == null || stack.isEmpty() || stack.getItem() == null) stackInfo = "empty";
                    else {
                        try { stackInfo = String.valueOf(stack.getItem().getRegistryName()); }
                        catch (RuntimeException ignored) { stackInfo = stack.getItem().getClass().getName(); }
                    }
                }
                NfrUiEnhancements.LOGGER.debug(
                        "Tooltip external draw: kind={}, x={}, y={}, width={}, height={}, stack={}, renderItemZ={}, depth={}, lighting={}",
                        node.kind(), x, y, safeWidth(), safeHeight(), stackInfo,
                        renderItem == null ? "n/a" : renderItem.zLevel, depth, lighting);
            }
            if (!item) {
                drawNode(node, x, y, font, maxWidth);
                return;
            }

            // Item rendering needs an isolated GUI state so ordinary and 3D items
            // cannot inherit the preceding panel's blend or depth state.
            try (ModernTooltipRenderer.CallerGlState ignored =
                         ModernTooltipRenderer.CallerGlState.capture()) {
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.enableTexture2D();
                    GlStateManager.enableAlpha();
                    GlStateManager.enableDepth();
                    GlStateManager.depthMask(true);
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderHelper.enableGUIStandardItemLighting();
                    drawNode(node, x, y, font, maxWidth);
                } finally {
                    RenderHelper.disableStandardItemLighting();
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    GlStateManager.popMatrix();
                }
            }
        }

        private int safeWidth() {
            try { return width(); } catch (RuntimeException ignored) { return -1; }
        }

        private int safeHeight() {
            try { return height(); } catch (RuntimeException ignored) { return -1; }
        }

        private static void drawNode(NfrTooltipApi.VisualNode value, int x, int y,
                                     FontRenderer font, int maxWidth) {
            RenderItem items = Minecraft.getMinecraft().getRenderItem();
            switch (value.kind()) {
                case ITEM:
                    NfrTooltipApi.ItemNode item = (NfrTooltipApi.ItemNode) value;
                    items.renderItemAndEffectIntoGUI(item.stack(), x, y);
                    if (item.showAmount()) {
                        items.renderItemOverlayIntoGUI(font, item.stack(), x, y, Long.toString(item.amount()));
                    }
                    break;
                case ITEM_ROW:
                    drawItemRow((NfrTooltipApi.ItemRowNode) value, x, y, font, maxWidth);
                    break;
                case TEXT:
                    NfrTooltipApi.TextNode text = (NfrTooltipApi.TextNode) value;
                    if (text.shadow()) font.drawStringWithShadow(text.text(), x, y, text.color());
                    else font.drawString(text.text(), x, y, text.color());
                    break;
                case SPACER:
                    break;
                case PREVIEW:
                    NfrTooltipApi.PreviewNode preview = (NfrTooltipApi.PreviewNode) value;
                    NfrTooltipApi.PreviewSize previewSize = previewContentSize(preview, font);
                    NfrTooltipApi.PreviewInsets insets = previewSize == null
                            ? NfrTooltipApi.PreviewInsets.NONE
                            : PreviewEffects.outsets(preview.request(), previewSize);
                    TooltipPreviewRenderers.render(preview, x + insets.left(), y + insets.top(), font);
                    break;
                case GROUP:
                    drawGroup((NfrTooltipApi.GroupNode) value, x, y, font, maxWidth);
                    break;
                case GRID:
                    drawGrid((NfrTooltipApi.GridNode) value, x, y, font, maxWidth);
                    break;
            }
        }

        private static int nodeWidth(NfrTooltipApi.VisualNode value, FontRenderer font) {
            return nodeWidth(value, font, Integer.MAX_VALUE);
        }

        private static int nodeWidth(NfrTooltipApi.VisualNode value, FontRenderer font,
                                     int maxWidth) {
            if (value instanceof NfrTooltipApi.PreviewNode) {
                NfrTooltipApi.PreviewNode preview = (NfrTooltipApi.PreviewNode) value;
                NfrTooltipApi.PreviewSize size = previewContentSize(preview, font);
                if (size != null) {
                    NfrTooltipApi.PreviewInsets insets = PreviewEffects.outsets(preview.request(), size);
                    return size.width() + insets.left() + insets.right();
                }
            }
            if (value instanceof NfrTooltipApi.ItemRowNode) {
                return itemRowMetrics((NfrTooltipApi.ItemRowNode) value, font, maxWidth)[0];
            }
            if (value instanceof NfrTooltipApi.GroupNode) {
                NfrTooltipApi.GroupNode group = (NfrTooltipApi.GroupNode) value;
                int result = 0;
                for (NfrTooltipApi.VisualNode child : group.children()) result = group.direction()
                        == NfrTooltipApi.LayoutDirection.HORIZONTAL
                        ? result + nodeWidth(child, font, Integer.MAX_VALUE)
                        : Math.max(result, nodeWidth(child, font, maxWidth));
                if (group.direction() == NfrTooltipApi.LayoutDirection.HORIZONTAL
                        && group.children().size() > 1) {
                    result += group.gap() * (group.children().size() - 1);
                }
                return result;
            }
            if (value instanceof NfrTooltipApi.GridNode) {
                NfrTooltipApi.GridNode grid = (NfrTooltipApi.GridNode) value;
                int columns = effectiveColumns(grid, font, maxWidth);
                int childLimit = columns == 1 ? maxWidth : Integer.MAX_VALUE;
                int[] widths = new int[columns];
                for (int index = 0; index < grid.children().size(); index++) {
                    int column = index % columns;
                    if (column < columns) widths[column] = Math.max(widths[column],
                            nodeWidth(grid.children().get(index), font, childLimit));
                }
                int result = 0;
                for (int width : widths) result += width;
                return result + Math.max(0, columns - 1) * grid.gap();
            }
            return value.width(font);
        }

        private static int nodeHeight(NfrTooltipApi.VisualNode value, FontRenderer font) {
            return nodeHeight(value, font, Integer.MAX_VALUE);
        }

        private static int nodeHeight(NfrTooltipApi.VisualNode value, FontRenderer font,
                                      int maxWidth) {
            if (value instanceof NfrTooltipApi.PreviewNode) {
                NfrTooltipApi.PreviewNode preview = (NfrTooltipApi.PreviewNode) value;
                NfrTooltipApi.PreviewSize size = previewContentSize(preview, font);
                if (size != null) {
                    NfrTooltipApi.PreviewInsets insets = PreviewEffects.outsets(preview.request(), size);
                    return size.height() + insets.top() + insets.bottom();
                }
            }
            if (value instanceof NfrTooltipApi.ItemRowNode) {
                return itemRowMetrics((NfrTooltipApi.ItemRowNode) value, font, maxWidth)[1];
            }
            if (value instanceof NfrTooltipApi.GroupNode) {
                NfrTooltipApi.GroupNode group = (NfrTooltipApi.GroupNode) value;
                int result = 0;
                for (NfrTooltipApi.VisualNode child : group.children()) result = group.direction()
                        == NfrTooltipApi.LayoutDirection.VERTICAL
                        ? result + nodeHeight(child, font, maxWidth)
                        : Math.max(result, nodeHeight(child, font, Integer.MAX_VALUE));
                if (group.direction() == NfrTooltipApi.LayoutDirection.VERTICAL
                        && group.children().size() > 1) {
                    result += group.gap() * (group.children().size() - 1);
                }
                return result;
            }
            if (value instanceof NfrTooltipApi.GridNode) {
                NfrTooltipApi.GridNode grid = (NfrTooltipApi.GridNode) value;
                int columns = effectiveColumns(grid, font, maxWidth);
                int childLimit = columns == 1 ? maxWidth : Integer.MAX_VALUE;
                int rows = (grid.children().size() + columns - 1) / columns;
                int result = 0;
                for (int row = 0; row < rows; row++) {
                    int rowHeight = 0;
                    for (int column = 0; column < columns; column++) {
                        int index = row * columns + column;
                        if (index < grid.children().size()) rowHeight = Math.max(rowHeight,
                                nodeHeight(grid.children().get(index), font, childLimit));
                    }
                    result += rowHeight;
                }
                return result + Math.max(0, rows - 1) * grid.gap();
            }
            return value.height(font);
        }

        private static NfrTooltipApi.PreviewSize previewContentSize(
                NfrTooltipApi.PreviewNode preview, FontRenderer font) {
            NfrTooltipApi.PreviewRenderer renderer = TooltipPreviewRenderers.find(preview.request());
            if (renderer == null) return null;
            try {
                NfrTooltipApi.PreviewSize measured = renderer.measure(preview.request(), font);
                if (measured == null) return null;
                int width = preview.width(font) > 0 ? preview.width(font) : measured.width();
                int height = preview.height(font) > 0 ? preview.height(font) : measured.height();
                return new NfrTooltipApi.PreviewSize(width, height);
            } catch (RuntimeException | LinkageError ignored) {
                return null;
            }
        }

        private static boolean requiresModelState(NfrTooltipApi.VisualNode value) {
            if (value.kind() == NfrTooltipApi.Kind.ITEM || value.kind() == NfrTooltipApi.Kind.ITEM_ROW
                    || value.kind() == NfrTooltipApi.Kind.PREVIEW) return true;
            if (value instanceof NfrTooltipApi.GroupNode) {
                for (NfrTooltipApi.VisualNode child : ((NfrTooltipApi.GroupNode) value).children())
                    if (requiresModelState(child)) return true;
            }
            if (value instanceof NfrTooltipApi.GridNode) {
                for (NfrTooltipApi.VisualNode child : ((NfrTooltipApi.GridNode) value).children())
                    if (requiresModelState(child)) return true;
            }
            return false;
        }

        private static void drawGroup(NfrTooltipApi.GroupNode group, int x, int y,
                                      FontRenderer font, int maxWidth) {
            int offset = 0;
            int totalWidth = nodeWidth(group, font, maxWidth);
            int totalHeight = nodeHeight(group, font, maxWidth);
            for (NfrTooltipApi.VisualNode child : group.children()) {
                int childLimit = group.direction() == NfrTooltipApi.LayoutDirection.VERTICAL
                        ? maxWidth : Integer.MAX_VALUE;
                int childWidth = nodeWidth(child, font, childLimit);
                int childHeight = nodeHeight(child, font, childLimit);
                int childX = x;
                int childY = y;
                if (group.direction() == NfrTooltipApi.LayoutDirection.HORIZONTAL) {
                    childX = x + offset;
                    childY = aligned(y, totalHeight, childHeight, group.alignment());
                    offset += childWidth + group.gap();
                } else {
                    childX = aligned(x, totalWidth, childWidth, group.alignment());
                    childY = y + offset;
                    offset += childHeight + group.gap();
                }
                drawNode(child, childX, childY, font, childLimit);
            }
        }

        private static void drawGrid(NfrTooltipApi.GridNode grid, int x, int y,
                                     FontRenderer font, int maxWidth) {
            int columns = effectiveColumns(grid, font, maxWidth);
            int[] columnWidths = new int[columns];
            int rows = (grid.children().size() + columns - 1) / columns;
            int[] rowHeights = new int[rows];
            int childLimit = columns == 1 ? maxWidth : Integer.MAX_VALUE;
            for (int index = 0; index < grid.children().size(); index++) {
                int column = index % columns;
                int row = index / columns;
                columnWidths[column] = Math.max(columnWidths[column],
                        nodeWidth(grid.children().get(index), font, childLimit));
                rowHeights[row] = Math.max(rowHeights[row],
                        nodeHeight(grid.children().get(index), font, childLimit));
            }
            int[] columnX = new int[columnWidths.length];
            for (int column = 1; column < columnWidths.length; column++)
                columnX[column] = columnX[column - 1] + columnWidths[column - 1] + grid.gap();
            int offsetY = 0;
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    int index = row * columns + column;
                    if (index >= grid.children().size()) break;
                    NfrTooltipApi.VisualNode child = grid.children().get(index);
                    drawNode(child, x + columnX[column], y + offsetY, font, columnWidths[column]);
                }
                offsetY += rowHeights[row] + grid.gap();
            }
        }

        private static int effectiveColumns(NfrTooltipApi.GridNode grid, FontRenderer font,
                                            int maxWidth) {
            int maximum = Math.max(1, Math.min(grid.columns(), grid.children().size()));
            if (maxWidth == Integer.MAX_VALUE) return maximum;
            for (int columns = maximum; columns > 1; columns--) {
                int[] widths = new int[columns];
                for (int index = 0; index < grid.children().size(); index++) {
                    int column = index % columns;
                    widths[column] = Math.max(widths[column],
                            nodeWidth(grid.children().get(index), font, Integer.MAX_VALUE));
                }
                int width = Math.max(0, columns - 1) * grid.gap();
                for (int columnWidth : widths) width += columnWidth;
                if (width <= maxWidth) return columns;
            }
            return 1;
        }

        private static int aligned(int origin, int available, int size,
                                   NfrTooltipApi.LayoutAlignment alignment) {
            if (alignment == NfrTooltipApi.LayoutAlignment.END) return origin + available - size;
            if (alignment == NfrTooltipApi.LayoutAlignment.CENTER) return origin + (available - size) / 2;
            return origin;
        }

        private static void drawItemRow(NfrTooltipApi.ItemRowNode row, int x, int y,
                                        FontRenderer font, int maxWidth) {
            int limit = normalizedWidthLimit(maxWidth);
            int offsetX = 0;
            int offsetY = 0;
            for (NfrTooltipApi.ItemNode child : row.items()) {
                if (offsetX > 0 && offsetX + 17 > limit) {
                    offsetX = 0;
                    offsetY += 17;
                }
                drawNode(child, x + offsetX, y + offsetY, font, maxWidth);
                offsetX += 17;
            }
            if (row.hasMore()) {
                int width = moreCellWidth(font);
                if (offsetX > 0 && offsetX + width > limit) {
                    offsetX = 0;
                    offsetY += 17;
                }
                font.drawStringWithShadow("...", x + offsetX + 2, y + offsetY + 2, 0xFFFFFF);
            }
        }
    }

    static int[] itemRowMetrics(NfrTooltipApi.ItemRowNode row, FontRenderer font, int maxWidth) {
        int limit = normalizedWidthLimit(maxWidth);
        int rowWidth = 0;
        int measuredWidth = 0;
        int rows = 1;
        for (int index = 0; index < row.items().size(); index++) {
            if (rowWidth > 0 && rowWidth + 17 > limit) {
                measuredWidth = Math.max(measuredWidth, rowWidth);
                rowWidth = 0;
                rows++;
            }
            rowWidth += 17;
        }
        if (row.hasMore()) {
            int width = moreCellWidth(font);
            if (rowWidth > 0 && rowWidth + width > limit) {
                measuredWidth = Math.max(measuredWidth, rowWidth);
                rowWidth = 0;
                rows++;
            }
            rowWidth += width;
        }
        measuredWidth = Math.max(measuredWidth, rowWidth);
        return new int[]{measuredWidth, rows * 17};
    }

    private static int normalizedWidthLimit(int maxWidth) {
        return maxWidth == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(1, maxWidth);
    }

    private static int moreCellWidth(FontRenderer font) {
        return font == null ? 10 : Math.max(1, font.getStringWidth("...") + 2);
    }

    void addAfter(int lineIndex, TooltipVisualBlock block) {
        if (block == null || block.width() <= 0 || block.height() <= 0
                || lineIndex < 0 || lineIndex >= afterLines.size()) return;
        afterLines.get(lineIndex).add(block);
    }

    void addSide(TooltipVisualBlock block) {
        if (block == null || block.width() <= 0 || block.height() <= 0) return;
        sideBlocks.add(block);
    }

    int sideWidth() {
        return sideLayout().width;
    }

    int sideHeight() {
        return sideLayout().height;
    }

    void drawSide(int x, int y, FontRenderer font) {
        for (SidePlacement placement : sideLayout().placements) {
            placement.block.draw(x + placement.x, y + placement.y, font);
        }
    }

    List<SidePlacement> sidePlacements() {
        return sideLayout().placements;
    }

    private SideLayout sideLayout() {
        List<TooltipLayoutEngine.Node> cells = new ArrayList<>();
        List<TooltipLayoutEngine.Leaf> leaves = new ArrayList<>();
        for (TooltipVisualBlock block : sideBlocks) {
            TooltipLayoutEngine.Leaf leaf = new TooltipLayoutEngine.Leaf(block.width() + 4, block.height());
            leaves.add(leaf);
            cells.add(leaf);
        }
        TooltipLayoutEngine.Wrap wrap = new TooltipLayoutEngine.Wrap(
                sideWidthLimit, 0, 4, TooltipLayoutEngine.Insets.none(), cells);
        TooltipLayoutEngine.Measurement measurement = wrap.measure(TooltipLayoutEngine.Constraints.unbounded());
        wrap.place(new TooltipLayoutEngine.Rect(0, 0, measurement.width, measurement.height));
        List<SidePlacement> placements = new ArrayList<>();
        for (int index = 0; index < leaves.size(); index++) {
            TooltipLayoutEngine.Rect bounds = leaves.get(index).bounds();
            placements.add(new SidePlacement(bounds.x, bounds.y, sideBlocks.get(index)));
        }
        return new SideLayout(measurement.width, measurement.height,
                Collections.unmodifiableList(placements));
    }

    private static final class SideLayout {
        final int width;
        final int height;
        final List<SidePlacement> placements;

        SideLayout(int width, int height, List<SidePlacement> placements) {
            this.width = width;
            this.height = height;
            this.placements = placements;
        }
    }

    List<TooltipVisualBlock> after(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= afterLines.size()) return Collections.emptyList();
        return afterLines.get(lineIndex);
    }

    List<VisualPlacement> placements() {
        List<VisualPlacement> result = new ArrayList<>();
        if (!sideBlocks.isEmpty() && !afterLines.isEmpty()) {
            result.add(new VisualPlacement(0, new TooltipVisualBlock() {
                @Override public int width() { return TooltipVisualPlan.this.sideWidth(); }
                @Override public int height() { return TooltipVisualPlan.this.sideHeight(); }
                @Override public void draw(int x, int y, FontRenderer font) {
                    TooltipVisualPlan.this.drawSide(x, y, font);
                }
                @Override public String debugLabel() { return "side-preview-flow"; }
            }));
        }
        for (int line = 0; line < afterLines.size(); line++) {
            for (TooltipVisualBlock block : afterLines.get(line)) {
                result.add(new VisualPlacement(line, block));
            }
        }
        return result;
    }

    static final class VisualPlacement {
        final int line;
        final TooltipVisualBlock block;

        VisualPlacement(int line, TooltipVisualBlock block) {
            this.line = line;
            this.block = block;
        }
    }

    static final class SidePlacement {
        final int x;
        final int y;
        final TooltipVisualBlock block;

        SidePlacement(int x, int y, TooltipVisualBlock block) {
            this.x = x;
            this.y = y;
            this.block = block;
        }
    }

    boolean hasAfter(int lineIndex) {
        return !after(lineIndex).isEmpty();
    }

    boolean hasAny() {
        if (!sideBlocks.isEmpty()) return true;
        for (List<TooltipVisualBlock> blocks : afterLines) {
            if (!blocks.isEmpty()) return true;
        }
        return false;
    }

    private int blockCount() {
        int count = sideBlocks.size();
        for (List<TooltipVisualBlock> blocks : afterLines) count += blocks.size();
        return count;
    }

    int maxWidth() {
        int width = 0;
        for (List<TooltipVisualBlock> blocks : afterLines) {
            for (TooltipVisualBlock block : blocks) width = Math.max(width, block.width());
        }
        return width;
    }

    int contentWidth() {
        return maxWidth() + sideWidth();
    }

    int contentWidth(int textWidth) {
        return sideWidth() + Math.max(Math.max(0, textWidth), maxWidth());
    }

    int totalHeight() {
        int height = 0;
        for (List<TooltipVisualBlock> blocks : afterLines) {
            for (TooltipVisualBlock block : blocks) height += block.height();
        }
        return height;
    }

    int contentHeight() {
        return Math.max(totalHeight(), sideHeight());
    }

    TooltipVisualPlan constrain(int maxWidth) {
        TooltipVisualPlan constrained = new TooltipVisualPlan(afterLines.size());
        constrained.sideWidthLimit = Math.max(1, maxWidth);
        for (TooltipVisualBlock block : sideBlocks) {
            constrained.sideBlocks.add(block.constrain(maxWidth));
        }
        for (int line = 0; line < afterLines.size(); line++) {
            for (TooltipVisualBlock block : afterLines.get(line)) {
                constrained.addAfter(line, block.constrain(maxWidth));
            }
        }
        return constrained;
    }

    TooltipVisualPlan remap(List<Integer> lastWrappedLineBySource, int wrappedLineCount) {
        TooltipVisualPlan remapped = new TooltipVisualPlan(wrappedLineCount);
        remapped.sideBlocks.addAll(sideBlocks);
        remapped.sideWidthLimit = sideWidthLimit;
        int count = Math.min(afterLines.size(), lastWrappedLineBySource.size());
        for (int sourceLine = 0; sourceLine < count; sourceLine++) {
            int targetLine = lastWrappedLineBySource.get(sourceLine);
            if (targetLine < 0 || targetLine >= wrappedLineCount) continue;
            remapped.afterLines.get(targetLine).addAll(afterLines.get(sourceLine));
        }
        return remapped;
    }

    private static boolean shouldDiagnostic(AtomicLong last) {
        if (!NfrUiEnhancements.LOGGER.isDebugEnabled()) return false;
        long now = System.nanoTime();
        long previous = last.get();
        if (now - previous < DIAGNOSTIC_INTERVAL_NANOS) return false;
        return last.compareAndSet(previous, now);
    }
}
