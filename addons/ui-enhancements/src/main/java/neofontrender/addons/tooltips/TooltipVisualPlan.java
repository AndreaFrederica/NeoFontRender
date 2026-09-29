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
import neofontrender.addons.build.UiBuildFeatures;
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
    private SideLayout sideLayoutCache;

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
        if (UiBuildFeatures.DIAGNOSTIC_LOGS && shouldDiagnostic(LAST_COLLECT_DIAGNOSTIC)) {
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
                if (UiBuildFeatures.DIAGNOSTIC_LOGS && shouldDiagnostic(LAST_ANCHOR_DIAGNOSTIC)) {
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
        private final TooltipVisualNodeLayout.Composition composition;
        private TooltipVisualNodeLayout.Result result;

        ExternalBlock(NfrTooltipApi.VisualNode node, FontRenderer font) {
            this(node, font, Integer.MAX_VALUE);
        }

        private ExternalBlock(NfrTooltipApi.VisualNode node, FontRenderer font, int maxWidth) {
            this(node, font, maxWidth, TooltipVisualNodeLayout.compose(node, font));
        }

        private ExternalBlock(NfrTooltipApi.VisualNode node, FontRenderer font, int maxWidth,
                              TooltipVisualNodeLayout.Composition composition) {
            this.node = node;
            this.font = font;
            this.maxWidth = maxWidth;
            this.composition = composition;
        }

        private TooltipVisualNodeLayout.Result result() {
            if (result == null) result = composition.layout(maxWidth);
            return result;
        }

        @Override public int width() { return result().width; }
        @Override public int height() { return result().height; }

        @Override public TooltipVisualBlock constrain(int width) {
            return new ExternalBlock(node, font, Math.max(1, width), composition);
        }

        @Override public String debugLabel() {
            if (node == null || node.kind() == null) return "visual";
            switch (node.kind()) {
                case ITEM: return "item";
                case ITEM_ROW: return "item-row";
                case PREVIEW:
                    if (node instanceof NfrTooltipApi.PreviewNode
                            && ((NfrTooltipApi.PreviewNode) node).request() != null) {
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

        @Override public List<TooltipVisualBlock.DebugBounds> debugBounds() {
            return result().debugBounds;
        }

        @Override public void draw(int x, int y, FontRenderer font) {
            TooltipVisualNodeLayout.Result layout = result();
            if (UiBuildFeatures.DIAGNOSTIC_LOGS && shouldDiagnostic(LAST_DRAW_DIAGNOSTIC)) {
                RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
                NfrUiEnhancements.LOGGER.debug(
                        "Tooltip external draw: kind={}, x={}, y={}, width={}, height={}, renderItemZ={}",
                        node == null ? "null" : node.kind(), x, y, layout.width, layout.height,
                        renderItem == null ? "n/a" : renderItem.zLevel);
            }
            if (!layout.requiresModelState) {
                layout.draw(x, y, font);
                return;
            }
            try (ModernTooltipRenderer.CallerGlState ignored =
                         ModernTooltipRenderer.CallerGlState.capture()) {
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
                GlStateManager.enableDepth();
                GlStateManager.depthMask(true);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                RenderHelper.enableGUIStandardItemLighting();
                try {
                    layout.draw(x, y, font);
                } finally {
                    RenderHelper.disableStandardItemLighting();
                }
            }
        }
    }

    void addAfter(int lineIndex, TooltipVisualBlock block) {
        if (block == null || block.width() <= 0 || block.height() <= 0
                || lineIndex < 0 || lineIndex >= afterLines.size()) return;
        afterLines.get(lineIndex).add(block);
    }

    void addSide(TooltipVisualBlock block) {
        if (block == null || block.width() <= 0 || block.height() <= 0) return;
        sideBlocks.add(block);
        sideLayoutCache = null;
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
        if (sideLayoutCache != null) return sideLayoutCache;
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
        sideLayoutCache = new SideLayout(measurement.width, measurement.height,
                Collections.unmodifiableList(placements));
        return sideLayoutCache;
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
        int budget = Math.max(5, Math.max(4, maxWidth) / 2);
        int blockBudget = Math.max(1, budget - 4);
        constrained.sideWidthLimit = budget;
        for (TooltipVisualBlock block : sideBlocks) {
            constrained.sideBlocks.add(block.constrain(blockBudget));
        }
        int sideWidth = constrained.sideWidth();
        int remaining = Math.max(1, maxWidth - sideWidth);
        for (int line = 0; line < afterLines.size(); line++) {
            for (TooltipVisualBlock block : afterLines.get(line)) {
                constrained.addAfter(line, block.constrain(remaining));
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
        if (!UiBuildFeatures.DIAGNOSTIC_LOGS) return false;
        if (!NfrUiEnhancements.LOGGER.isDebugEnabled()) return false;
        long now = System.nanoTime();
        long previous = last.get();
        if (now - previous < DIAGNOSTIC_INTERVAL_NANOS) return false;
        return last.compareAndSet(previous, now);
    }
}
