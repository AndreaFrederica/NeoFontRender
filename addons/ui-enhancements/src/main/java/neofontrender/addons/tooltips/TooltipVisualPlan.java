package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Visual blocks anchored after real tooltip lines, independent of whitespace placeholders. */
final class TooltipVisualPlan {
    private final List<List<TooltipVisualBlock>> afterLines;

    TooltipVisualPlan(int lineCount) {
        afterLines = new ArrayList<>(Math.max(0, lineCount));
        for (int i = 0; i < lineCount; i++) afterLines.add(new ArrayList<>());
    }

    static TooltipVisualPlan collect(ItemStack stack, List<String> lines, FontRenderer font,
                                     int maxWidth) {
        TooltipVisualPlan plan = new TooltipVisualPlan(lines == null ? 0 : lines.size());
        if (lines == null || lines.isEmpty()) return plan;

        ThaumcraftAspectTooltipCompat.Chart chart =
                ThaumcraftAspectTooltipCompat.chart(stack, font, maxWidth);
        if (chart != null) plan.addAfter(0, chart);
        QuarkTooltipVisuals.populate(plan, stack, lines, font, maxWidth);
        return plan;
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
}
