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

        TooltipVisualPlan constrained = plan.constrain(70);

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

        TooltipVisualPlan constrained = plan.constrain(70);

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

        TooltipVisualPlan constrained = plan.constrain(70);
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

        assertEquals(95, TooltipVisualPlan.itemRowMetrics(row, null, Integer.MAX_VALUE)[0]);
        assertEquals(17, TooltipVisualPlan.itemRowMetrics(row, null, Integer.MAX_VALUE)[1]);
        assertEquals(34, TooltipVisualPlan.itemRowMetrics(row, null, 40)[0]);
        assertEquals(51, TooltipVisualPlan.itemRowMetrics(row, null, 40)[1]);
        assertEquals(17, TooltipVisualPlan.itemRowMetrics(row, null, 20)[0]);
        assertEquals(102, TooltipVisualPlan.itemRowMetrics(row, null, 20)[1]);
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
