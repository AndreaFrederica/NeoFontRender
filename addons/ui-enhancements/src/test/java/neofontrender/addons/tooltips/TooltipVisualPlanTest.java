package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TooltipVisualPlanTest {
    @Test
    void previewPanelReservesInsetsWhenWrappingAndEmptyPlansStayEmpty() {
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.previewPanelFrameEnabled = true;
            TooltipConfig.previewPanelBackgroundEnabled = false;
            TooltipVisualPlan empty = new TooltipVisualPlan(1);
            assertEquals(0, empty.sideWidth());
            assertEquals(0, empty.sideHeight());
            TooltipVisualPlan plan = new TooltipVisualPlan(1);
            plan.addSide(new StubBlock(30, 10));
            plan.addSide(new StubBlock(30, 12));
            TooltipVisualPlan constrained = plan.constrain(140);
            // 3 px padding and the existing 4 px text gap force the second model down.
            assertEquals(40, constrained.sideWidth());
            assertEquals(32, constrained.sideHeight());
            assertEquals(3, constrained.sidePlacements().get(0).x);
            assertEquals(3, constrained.sidePlacements().get(0).y);
            assertEquals(17, constrained.sidePlacements().get(1).y);
            TooltipConfig.previewPanelFrameEnabled = false;
            TooltipConfig.previewPanelBackgroundEnabled = true;
            assertEquals(40, plan.constrain(140).sideWidth());
            TooltipConfig.previewPanelBackgroundEnabled = false;
            assertEquals(68, plan.constrain(140).sideWidth());
        } finally {
            original.restore();
        }
    }

    @Test
    void verticalCenteringAccountsForInlineBlocksAndOnlyMovesTheShorterColumn() {
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.titleBreak = true;
            TooltipConfig.dividerTopMargin = 0;
            TooltipConfig.dividerBottomMargin = 3;
            TooltipConfig.previewPanelFrameEnabled = false;
            TooltipConfig.previewPanelBackgroundEnabled = false;
            TooltipVisualPlan plan = new TooltipVisualPlan(3);
            plan.addSide(new StubBlock(30, 64));
            plan.addAfter(1, new StubBlock(40, 100));
            int textHeight = TooltipLayout.textFlowHeight(Arrays.asList(22, 10, 10), 1,
                    Arrays.asList("Sword", "Details", "Mod"), plan);
            assertEquals(146, textHeight);
            TooltipConfig.sideAlignment = "top";
            assertEquals(0, TooltipLayout.verticalOffset(64, textHeight));
            TooltipConfig.sideAlignment = "center";
            assertEquals(41, TooltipLayout.verticalOffset(64, textHeight));
            assertEquals(0, TooltipLayout.verticalOffset(textHeight, 64));
            assertEquals(16, TooltipLayout.verticalOffset(32, 64));
            assertEquals(0, TooltipLayout.verticalOffset(64, 64));
            assertEquals(0, TooltipLayout.verticalOffset(textHeight, 0));
        } finally {
            original.restore();
        }
    }

    @Test
    void remapsVisualToTheLastWrappedPartOfItsSourceLine() {
        TooltipVisualPlan source = new TooltipVisualPlan(3);
        StubBlock block = new StubBlock(45, 10);
        source.addAfter(1, block);

        TooltipVisualPlan wrapped = source.remap(Arrays.asList(1, 4, 5), 6);

        assertEquals(0, wrapped.after(1).size());
        assertEquals(1, wrapped.after(4).size());
        assertSame(block, wrapped.after(4).get(0));
    }

    @Test
    void measuresBlocksWithoutCreatingTextRows() {
        TooltipVisualPlan plan = new TooltipVisualPlan(3);
        plan.addAfter(0, new StubBlock(45, 10));
        plan.addAfter(1, new StubBlock(17, 18));

        assertEquals(45, plan.maxWidth());
        assertEquals(28, plan.totalHeight());
    }

    @Test
    void wrapsSideBlocksWithinTheAvailableWidth() {
        TooltipVisualPlan plan = new TooltipVisualPlan(1);
        plan.addSide(new StubBlock(30, 10));
        plan.addSide(new StubBlock(30, 12));
        plan.addSide(new StubBlock(30, 8));

        TooltipVisualPlan constrained = plan.constrain(140);

        assertEquals(68, constrained.sideWidth());
        assertEquals(24, constrained.sideHeight());
        assertEquals(108, constrained.contentWidth(40));
    }

    @Test
    void exposesSidePreviewsAsOneFlowBlockForComponentRenderers() {
        TooltipVisualPlan plan = new TooltipVisualPlan(2);
        plan.addSide(new StubBlock(30, 10));
        plan.addSide(new StubBlock(30, 12));
        plan.addSide(new StubBlock(30, 8));

        TooltipVisualPlan constrained = plan.constrain(140);

        assertEquals(1, constrained.placements().size());
        assertEquals(0, constrained.placements().get(0).line);
        assertEquals(68, constrained.placements().get(0).block.width());
        assertEquals(24, constrained.placements().get(0).block.height());
    }

    @Test
    void exposesEachSideBlockPlacementForDebugOverlay() {
        TooltipVisualPlan plan = new TooltipVisualPlan(1);
        plan.addSide(new StubBlock(30, 10));
        plan.addSide(new StubBlock(30, 12));
        plan.addSide(new StubBlock(30, 8));

        TooltipVisualPlan constrained = plan.constrain(140);
        assertEquals(3, constrained.sidePlacements().size());
        assertEquals(0, constrained.sidePlacements().get(0).x);
        assertEquals(34, constrained.sidePlacements().get(1).x);
        assertEquals(0, constrained.sidePlacements().get(2).x);
        assertEquals(16, constrained.sidePlacements().get(2).y);
    }

    @Test
    void wrapsItemRowsAndKeepsTheMoreIndicatorInsideTheConstraint() {
        NfrTooltipApi.ItemNode item = new NfrTooltipApi.ItemNode(ItemStack.EMPTY, 1, false);
        NfrTooltipApi.ItemRowNode row = new NfrTooltipApi.ItemRowNode(
                Arrays.asList(item, item, item, item, item), true);

        TooltipVisualNodeLayout.Composition composition = TooltipVisualNodeLayout.compose(row, null);
        TooltipVisualNodeLayout.Result unbounded = composition.layout(Integer.MAX_VALUE);
        assertEquals(95, unbounded.width);
        assertEquals(17, unbounded.height);
        TooltipVisualNodeLayout.Result constrained = TooltipVisualNodeLayout.compose(row, null).layout(40);
        assertEquals(34, constrained.width);
        assertEquals(51, constrained.height);
        TooltipVisualNodeLayout.Result narrow = TooltipVisualNodeLayout.compose(row, null).layout(20);
        assertEquals(17, narrow.width);
        assertEquals(102, narrow.height);
    }

    @Test
    void retainedCompositionExposesNestedDebugBounds() {
        NfrTooltipApi.GroupNode group = new NfrTooltipApi.GroupNode(
                Arrays.asList(new NfrTooltipApi.TextNode("label", 0xFFFFFFFF, false),
                        new NfrTooltipApi.ItemNode(ItemStack.EMPTY, 1, false)),
                NfrTooltipApi.LayoutDirection.HORIZONTAL);
        TooltipVisualNodeLayout.Result result = TooltipVisualNodeLayout.compose(group, null).layout(100);
        assertEquals(2, result.debugBounds.stream()
                .filter(value -> "text".equals(value.label) || "item".equals(value.label)).count());
        assertEquals(0, result.debugBounds.get(0).x);
    }

    private static final class StubBlock implements TooltipVisualBlock {
        final int width;
        final int height;

        StubBlock(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override public int width() { return width; }
        @Override public int height() { return height; }
        @Override public void draw(int x, int y, FontRenderer font) {}
    }
}
