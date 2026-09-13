package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.RenderHelper;
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
    private final List<List<TooltipVisualBlock>> afterLines;

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
                if (node != null) plan.addAfter(Math.max(0, lines.size() - 1), new ExternalBlock(node, font));
            }
        }

        if (lines != null && !lines.isEmpty()) {
            ThaumcraftAspectTooltipCompat.Chart chart =
                    ThaumcraftAspectTooltipCompat.chart(stack, font, maxWidth);
            if (chart != null) plan.addAfter(0, chart);
            QuarkTooltipVisuals.populate(plan, stack, lines, font, maxWidth);
        }
        if (shouldDiagnostic(LAST_COLLECT_DIAGNOSTIC)) {
            NfrUiEnhancements.LOGGER.info(
                    "Tooltip visual plan: lines={}, externalNodes={}, blocks={}, maxWidth={}, totalHeight={}, hasAny={}, maxConstraint={}",
                    lines == null ? 0 : lines.size(), external == null ? 0 : external.size(),
                    plan.blockCount(), plan.maxWidth(), plan.totalHeight(), plan.hasAny(), maxWidth);
        }
        return plan;
    }

    private static final class ExternalBlock implements TooltipVisualBlock {
        private final NfrTooltipApi.VisualNode node; private final FontRenderer font;
        ExternalBlock(NfrTooltipApi.VisualNode node, FontRenderer font) { this.node = node; this.font = font; }
        public int width() { return node.width(font); }
        public int height() { return node.height(font); }
        public void draw(int x, int y, FontRenderer font) {
            boolean item = node.kind() == NfrTooltipApi.Kind.ITEM || node.kind() == NfrTooltipApi.Kind.ITEM_ROW;
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
                NfrUiEnhancements.LOGGER.info(
                        "Tooltip external draw: kind={}, x={}, y={}, width={}, height={}, stack={}, renderItemZ={}, depth={}, lighting={}",
                        node.kind(), x, y, safeWidth(), safeHeight(), stackInfo,
                        renderItem == null ? "n/a" : renderItem.zLevel, depth, lighting);
            }
            if (item) RenderHelper.enableGUIStandardItemLighting();
            try { drawNode(node, x, y, font); }
            finally { if (item) RenderHelper.disableStandardItemLighting(); }
        }

        private int safeWidth() {
            try { return width(); } catch (RuntimeException ignored) { return -1; }
        }

        private int safeHeight() {
            try { return height(); } catch (RuntimeException ignored) { return -1; }
        }

        private static void drawNode(NfrTooltipApi.VisualNode value, int x, int y, FontRenderer font) {
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
                    NfrTooltipApi.ItemRowNode row = (NfrTooltipApi.ItemRowNode) value;
                    int offset = 0;
                    for (NfrTooltipApi.ItemNode child : row.items()) {
                        drawNode(child, x + offset, y, font);
                        offset += 17;
                    }
                    if (row.hasMore()) font.drawStringWithShadow("...", x + offset + 2, y + 2, 0xFFFFFF);
                    break;
                case TEXT:
                    NfrTooltipApi.TextNode text = (NfrTooltipApi.TextNode) value;
                    if (text.shadow()) font.drawStringWithShadow(text.text(), x, y, text.color());
                    else font.drawString(text.text(), x, y, text.color());
                    break;
                case SPACER:
                    break;
            }
        }
    }

    void addAfter(int lineIndex, TooltipVisualBlock block) {
        if (block == null || block.width() <= 0 || block.height() <= 0
                || lineIndex < 0 || lineIndex >= afterLines.size()) return;
        afterLines.get(lineIndex).add(block);
    }

    List<TooltipVisualBlock> after(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= afterLines.size()) return Collections.emptyList();
        return afterLines.get(lineIndex);
    }

    boolean hasAfter(int lineIndex) {
        return !after(lineIndex).isEmpty();
    }

    boolean hasAny() {
        for (List<TooltipVisualBlock> blocks : afterLines) {
            if (!blocks.isEmpty()) return true;
        }
        return false;
    }

    private int blockCount() {
        int count = 0;
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

    int totalHeight() {
        int height = 0;
        for (List<TooltipVisualBlock> blocks : afterLines) {
            for (TooltipVisualBlock block : blocks) height += block.height();
        }
        return height;
    }

    TooltipVisualPlan constrain(int maxWidth) {
        TooltipVisualPlan constrained = new TooltipVisualPlan(afterLines.size());
        for (int line = 0; line < afterLines.size(); line++) {
            for (TooltipVisualBlock block : afterLines.get(line)) {
                constrained.addAfter(line, block.constrain(maxWidth));
            }
        }
        return constrained;
    }

    TooltipVisualPlan remap(List<Integer> lastWrappedLineBySource, int wrappedLineCount) {
        TooltipVisualPlan remapped = new TooltipVisualPlan(wrappedLineCount);
        int count = Math.min(afterLines.size(), lastWrappedLineBySource.size());
        for (int sourceLine = 0; sourceLine < count; sourceLine++) {
            int targetLine = lastWrappedLineBySource.get(sourceLine);
            if (targetLine < 0 || targetLine >= wrappedLineCount) continue;
            remapped.afterLines.get(targetLine).addAll(afterLines.get(sourceLine));
        }
        return remapped;
    }

    private static boolean shouldDiagnostic(AtomicLong last) {
        long now = System.nanoTime();
        long previous = last.get();
        if (now - previous < DIAGNOSTIC_INTERVAL_NANOS) return false;
        return last.compareAndSet(previous, now);
    }
}
