package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
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
